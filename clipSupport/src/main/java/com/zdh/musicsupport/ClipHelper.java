package com.zdh.musicsupport;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * author: ZDH
 * Date: 2022/3/26
 * Description:
 */
public class ClipHelper {

    MediaCodec mDecoder;
    MediaFormat mFormat;
    int trackIndex = -1;
    ExecutorService mExecutorService;

    public ClipHelper() {
        mExecutorService = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new ArrayBlockingQueue(10));

    }

    private void initCodec() {
        try {
            mDecoder = MediaCodec.createDecoderByType(mFormat.getString(MediaFormat.KEY_MIME));
            mDecoder.configure(mFormat, null, null, 0);
            mDecoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findAndSelectAudioTrack(MediaExtractor mediaExtractor) {
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            if (mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).contains("audio/")) {
                trackIndex = i;
                mFormat = mediaExtractor.getTrackFormat(i);
                mediaExtractor.selectTrack(i);
                return i;
            }
        }
        return -1;
    }

    private int findTrack(MediaExtractor mediaExtractor, boolean audio) {
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            String mime = mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.contains("audio/")) {
                    trackIndex = i;
                    mFormat = mediaExtractor.getTrackFormat(i);
                    mediaExtractor.selectTrack(i);
                    return i;
                }
            } else {
                if (mime.contains("video/")) {
                    trackIndex = i;
                    mFormat = mediaExtractor.getTrackFormat(i);
                    mediaExtractor.selectTrack(i);
                    return i;
                }
            }

        }
        return -1;
    }

    private static float normalizeVolume(int volume) {
        return volume / 100f * 1;
    }



    public void mixVideoAndAudio(String videoInput, String audioInput, String outputPath,
                                 final Integer startTimeUs, final Integer endTimeUs,
                                 int videoVolume, int audioVolume) {
        File cacheDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        File videoPcm = new File(cacheDir, "videp.pcm");
        File audioPcm = new File(cacheDir, "audio.pcm");
        File mixPcm = new File(cacheDir, "mix.pcm");
        File wavFile = new File(cacheDir, "音频合成后.wav");
        try {
            clipDecodeToPcm(videoInput, videoPcm.getAbsolutePath(), startTimeUs, endTimeUs);
            clipDecodeToPcm(audioInput, audioPcm.getAbsolutePath(), startTimeUs, endTimeUs);
            mixPcm(videoPcm.getAbsolutePath(), audioPcm.getAbsolutePath(), mixPcm.getAbsolutePath()
                    , videoVolume, audioVolume);
            new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                    2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixPcm.getAbsolutePath()
                    , wavFile.getAbsolutePath());
            mixVideoAndAudioInner(videoInput, wavFile.getAbsolutePath(), outputPath, startTimeUs, endTimeUs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    private void mixVideoAndAudioInner(String videoInput, String wavPath, String outputPath, Integer startTimeUs, Integer endTimeUs) throws IOException {
        // 混合
        MediaMuxer mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        MediaFormat videoFormat, audioFormat;
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(videoInput);
        int videoTrack = findTrack(mediaExtractor, false);
        int audioTrack = findTrack(mediaExtractor, true);
        videoFormat = mediaExtractor.getTrackFormat(videoTrack);
        audioFormat = mediaExtractor.getTrackFormat(audioTrack);
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
        int audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        videoTrack = mediaMuxer.addTrack(videoFormat);
        audioTrack = mediaMuxer.addTrack(audioFormat);
        mediaMuxer.start();

        //***********音频合成文件***********
        MediaExtractor pcmExtractor = new MediaExtractor();
        pcmExtractor.setDataSource(wavPath);
        int trackIndex = findTrack(pcmExtractor, true);
        MediaFormat pcmTrackFormat = pcmExtractor.getTrackFormat(trackIndex);
        pcmExtractor.selectTrack(trackIndex);

        //最大一帧的 大小
        int maxBufferSize = 0;
        if (pcmTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        } else {
            maxBufferSize = 100 * 1000;
        }

        //********* 混音后的mediaFormat 信息 ********
        MediaFormat mixAudioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2);
        mixAudioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize);
        mixAudioFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate);
//            音质等级
        mixAudioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        // **** 音频编码 ****
        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        encoder.configure(mixAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

//            容器
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxBufferSize);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean eof = false;
        while (!eof) {
            int index = encoder.dequeueInputBuffer(1000);
            if (index >= 0) {
                long sampleTimes = pcmExtractor.getSampleTime();
                if (sampleTimes < 0) {
                    // pts小于0  来到了文件末尾 通知编码器  不用编码了
                    encoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    info.size = pcmExtractor.readSampleData(buffer, 0);
                    info.presentationTimeUs = sampleTimes;
                    info.flags = pcmExtractor.getSampleFlags();
                    ByteBuffer inputBuffer = encoder.getInputBuffer(index);
                    inputBuffer.clear();
                    inputBuffer.put(buffer);
                    inputBuffer.position(0);
                    encoder.queueInputBuffer(index, 0, info.size, sampleTimes, info.flags);
                    // 读下一帧
                    pcmExtractor.advance();
                }
            }
            index = encoder.dequeueOutputBuffer(info, 1000);
            while (index >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    eof = true;
                    break;
                }
                ByteBuffer outputBuffer = encoder.getOutputBuffer(index);
                mediaMuxer.writeSampleData(audioTrack, outputBuffer, info);
                outputBuffer.clear();
                encoder.releaseOutputBuffer(index, false);
                index = encoder.dequeueOutputBuffer(info, 1000);
            }
        }

        /**
         * 视频剪辑
         */
        mediaExtractor.unselectTrack(audioTrack);
        mediaExtractor.selectTrack(videoTrack);
        mediaExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        buffer = ByteBuffer.allocateDirect(maxBufferSize);
        while (true) {
            long sampleTimes = mediaExtractor.getSampleTime();
            if (sampleTimes == -1) {
                break;
            } else if (sampleTimes < startTimeUs) {
                mediaExtractor.advance();
                continue;
            } else if (endTimeUs != 0 && sampleTimes > endTimeUs) {
                break;
            }
            info.size = mediaExtractor.readSampleData(buffer, 0);
            if (info.size < 0) {
                break;
            }
            // 600 表示初始化过程需要的时间
            info.presentationTimeUs = sampleTimes - startTimeUs + 600;
            info.flags = mediaExtractor.getSampleFlags();
            mediaMuxer.writeSampleData(videoTrack, buffer, info);
            mediaExtractor.advance();
        }
        mediaMuxer.release();
        mediaExtractor.release();
        encoder.stop();
        encoder.release();
        pcmExtractor.release();
    }

    /**
     * 混音
     *
     * @param videoPath
     * @param audioPath
     * @param startTime
     * @param endTime
     */
    public void mixWithThread(String videoPath, String audioPath, int startTime, int endTime) {
        mExecutorService.submit(() -> {
            mix(videoPath, audioPath, startTime, endTime);
        });
    }

    public void mix(String videoPath, String audioPath, int startTime, int endTime) {
        File vfile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "video.pcm");
        File afile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "audio.pcm");
        clip(videoPath, vfile.getAbsolutePath(), startTime, endTime);
        clip(audioPath, afile.getAbsolutePath(), startTime, endTime);
        String mixPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/mix.pcm";
        try {
            mixPcm(vfile.getAbsolutePath(), afile.getAbsolutePath(), mixPath, 100, 100);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File wavFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "output.mp3");
        new PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO,
                2, AudioFormat.ENCODING_PCM_16BIT).pcmToWav(mixPath
                , wavFile.getAbsolutePath());
    }

    private void mixPcm(String videoPath, String audioPath, String mixPath, int vVolume, int aVolume) throws IOException {
        File videoFile = new File(videoPath);
        File audioFile = new File(audioPath);
        File mixFile = new File(mixPath);
        FileInputStream vfis = new FileInputStream(videoFile);
        FileInputStream afis = new FileInputStream(audioFile);
        FileOutputStream mfos = new FileOutputStream(mixFile);
        byte[] vbs = new byte[2048];
        byte[] abs = new byte[2048];
        byte[] mixBytes = new byte[2048];
        short vData, aData;
        int mixData;
        boolean vEnd = false, aEnd = false;
        float v = normalizeVolume(vVolume);
        float a = normalizeVolume(aVolume);
        while (!vEnd || !aEnd) {
            if (!vEnd) {
                vEnd = (vfis.read(vbs)) == -1;
                System.arraycopy(vbs, 0, mixBytes, 0, vbs.length);
            }
            if (!aEnd) {
                aEnd = (afis.read(abs)) == -1;
                for (int i = 0; i < abs.length; i += 2) {
                    vData = (short) ((vbs[i] & 0xff) | ((vbs[i + 1] & 0xff) << 8));
                    aData = (short) ((abs[i] & 0xff) | ((abs[i + 1] & 0xff) << 8));
                    float af = aData * a;
                    float vf = vData * v;
//                    Log.e("AudioClip", "mixInner: af " + af + "     vf " + vf);
                    mixData = (int) (af + vf);
                    // 混合数据越界
                    if (mixData > 32767) {
                        mixData = 32767;
                    } else if (mixData < -32768) {
                        mixData = -32768;
                    }
                    mixBytes[i] = (byte) (mixData & 0xff);
                    mixBytes[i + 1] = (byte) ((mixData >>> 8) & 0xff);
                }
                mfos.write(mixBytes);
            }
        }
        afis.close();
        vfis.close();
        mfos.close();
    }

    /**
     * 剪辑
     *
     * @param outputPath
     * @param startTime
     * @param endTime
     */
    public void clipWithIOThread(String inputPath, String outputPath, int startTime, int endTime) {
        mExecutorService.submit(() -> {
            try {
                clipDecodeToPcm(inputPath, outputPath, startTime, endTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 剪辑
     *
     * @param outputPath
     * @param startTime
     * @param endTime
     */
    public void clip(String inputPath, String outputPath, int startTime, int endTime) {
        try {
            clipDecodeToPcm(inputPath, outputPath, startTime, endTime);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("WrongConstant")
    private void clipDecodeToPcm(String inputPath, String outputPath, int startTime, int endTime) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(inputPath);
            findAndSelectAudioTrack(mediaExtractor);
            initCodec();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int maxInputSize = 100_00;
        if (mFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxInputSize = mFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(maxInputSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        int index = 0;

        /* file */
        File file = new File(outputPath);
        FileChannel fileChannel = null;
        fileChannel = new FileOutputStream(file).getChannel();
        while (true) {
            index = mDecoder.dequeueInputBuffer(10000);
            if (index >= 0) {
                // pts
                long sampeTimes = mediaExtractor.getSampleTime();
                if (sampeTimes == -1) {
                    break;
                } else if (sampeTimes < startTime) {
                    mediaExtractor.advance();
                    continue;
                } else if (sampeTimes > endTime) {
                    break;
                }
                bufferInfo.size = mediaExtractor.readSampleData(buffer, 0);
                bufferInfo.presentationTimeUs = sampeTimes;
                bufferInfo.flags = mediaExtractor.getSampleFlags();

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes, 0, buffer.remaining());
                ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
                inputBuffer.put(bytes);
                mDecoder.queueInputBuffer(index, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
                mediaExtractor.advance();
            }
            index = mDecoder.dequeueOutputBuffer(bufferInfo, 1000);
            while (index >= 0) {
                ByteBuffer byteBuffer = mDecoder.getOutputBuffer(index);
                fileChannel.write(byteBuffer);
                mDecoder.releaseOutputBuffer(index, false);
                index = mDecoder.dequeueOutputBuffer(bufferInfo, 1000);
            }
        }
        fileChannel.close();
        mediaExtractor.release();
        mDecoder.stop();
        mDecoder.release();

    }
}
