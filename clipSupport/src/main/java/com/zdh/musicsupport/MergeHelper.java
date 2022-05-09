package com.zdh.musicsupport;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * author: ZDH
 * Date: 2022/3/28
 * Description:
 */
public class MergeHelper {

    /**
     * 合并视频
     *
     * @param videoPath
     * @param appendVideoPath
     * @throws IOException
     */
//    public void appendVideo(String inputPath1, String inputPath2, String outputPath) throws IOException {
//        MediaMuxer mediaMuxer = new MediaMuxer(outputPath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        MediaExtractor videoExtractor1 = new MediaExtractor();
//        videoExtractor1.setDataSource(inputPath1);
//
//        MediaExtractor videoExtractor2 = new MediaExtractor();
//        videoExtractor2.setDataSource(inputPath2);
//
//        int videoTrackIndex = -1;
//        int audioTrackIndex = -1;
//        long file1_duration = 0L;
//
//        int sourceVideoTrack1 = -1;
//        int sourceAudioTrack1 = -1;
//        for (int index = 0; index < videoExtractor1.getTrackCount(); index++) {
//            MediaFormat format = videoExtractor1.getTrackFormat(index);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            file1_duration = format.getLong(MediaFormat.KEY_DURATION);
//            if (mime.startsWith("video/")) {
//                sourceVideoTrack1 = index;
//                videoTrackIndex = mediaMuxer.addTrack(format);
//            } else if (mime.startsWith("audio/")) {
//                sourceAudioTrack1 = index;
//                audioTrackIndex = mediaMuxer.addTrack(format);
//            }
//        }
//
//        int sourceVideoTrack2 = -1;
//        int sourceAudioTrack2 = -1;
//        for (int index = 0; index < videoExtractor2.getTrackCount(); index++) {
//            MediaFormat format = videoExtractor2.getTrackFormat(index);
//            String mime = format.getString(MediaFormat.KEY_MIME);
//            if (mime.startsWith("video/")) {
//                sourceVideoTrack2 = index;
//            } else if (mime.startsWith("audio/")) {
//                sourceAudioTrack2 = index;
//            }
//        }
//
//        mediaMuxer.start();
//        //1.write first video track into muxer.
//        videoExtractor1.selectTrack(sourceVideoTrack1);
//        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//        info.presentationTimeUs = 0;
//        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
//        int sampleSize = 0;
//        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
//            byte[] data = new byte[buffer.remaining()];
//
//            buffer.get(data);
//            info.offset = 0;
//            info.size = sampleSize;
//            info.flags = videoExtractor1.getSampleFlags();
//            info.presentationTimeUs = videoExtractor1.getSampleTime();
//            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
//            videoExtractor1.advance();
//        }
//
//        //2.write first audio track into muxer.
//        videoExtractor1.unselectTrack(sourceVideoTrack1);
//        videoExtractor1.selectTrack(sourceAudioTrack1);
//        info = new MediaCodec.BufferInfo();
//        info.presentationTimeUs = 0;
//        buffer = ByteBuffer.allocate(500 * 1024);
//        sampleSize = 0;
//        while ((sampleSize = videoExtractor1.readSampleData(buffer, 0)) > 0) {
//            info.offset = 0;
//            info.size = sampleSize;
//            info.flags = videoExtractor1.getSampleFlags();
////            byte[] data = new byte[buffer.remaining()];
////            buffer.get(data);
////            FileUtils.writeBytes(data);
////            FileUtils.writeContent(data);
//            info.presentationTimeUs = videoExtractor1.getSampleTime();
////            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
//            videoExtractor1.advance();
//        }
//
//        //3.write second video track into muxer.
//        videoExtractor2.selectTrack(sourceVideoTrack2);
//        info = new MediaCodec.BufferInfo();
//        info.presentationTimeUs = 0;
//        buffer = ByteBuffer.allocate(500 * 1024);
//        sampleSize = 0;
//        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
//            info.offset = 0;
//            info.size = sampleSize;
//            info.flags = videoExtractor2.getSampleFlags();
//            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
//            mediaMuxer.writeSampleData(videoTrackIndex, buffer, info);
//            videoExtractor2.advance();
//        }
//
//        //4.write second audio track into muxer.
//        videoExtractor2.unselectTrack(sourceVideoTrack2);
//        videoExtractor2.selectTrack(sourceAudioTrack2);
//        info = new MediaCodec.BufferInfo();
//        info.presentationTimeUs = 0;
//        buffer = ByteBuffer.allocate(500 * 1024);
//        sampleSize = 0;
//        while ((sampleSize = videoExtractor2.readSampleData(buffer, 0)) > 0) {
//            info.offset = 0;
//            info.size = sampleSize;
//            info.flags = videoExtractor2.getSampleFlags();
//            info.presentationTimeUs = videoExtractor2.getSampleTime() + file1_duration;
//            mediaMuxer.writeSampleData(audioTrackIndex, buffer, info);
//            videoExtractor2.advance();
//        }
//
//        videoExtractor1.release();
//        videoExtractor2.release();
//        mediaMuxer.stop();
//        mediaMuxer.release();
//    }
    public void appendVideo(String videoPath, String appendVideoPath,String outputPath) throws IOException {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor appendVideoExtractor = new MediaExtractor();
        videoExtractor.setDataSource(videoPath);
        appendVideoExtractor.setDataSource(appendVideoPath);
        // 视频流索引
        int videoIndex = findTrack(videoExtractor,false);
        int audioIndex = findTrack(videoExtractor,true);
        MediaFormat videoFormat = videoExtractor.getTrackFormat(videoIndex);
        MediaFormat audioFormat = videoExtractor.getTrackFormat(audioIndex);
        long videoDuration = videoFormat.getLong(MediaFormat.KEY_DURATION);
        // 添加轨道
        // 合成
        MediaMuxer mediaMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int videoTrack = mediaMuxer.addTrack(videoFormat);
        int audioTrack = mediaMuxer.addTrack(audioFormat);
        mediaMuxer.start();

        // 写入第一个文件数据到合成器
        copyToMuxer(videoExtractor, mediaMuxer, videoIndex, videoTrack,0);
        // 会出bug
//        copyToMuxer(videoExtractor, mediaMuxer, audioIndex, audioTrack,0);

        // 写入第二个文件数据到合成器
        videoIndex = findTrack(appendVideoExtractor,false);
        audioIndex = findTrack(appendVideoExtractor,true);
        copyToMuxer(appendVideoExtractor,mediaMuxer,videoIndex,videoTrack,videoDuration);
        copyToMuxer(appendVideoExtractor,mediaMuxer,audioIndex,audioTrack,videoDuration);

        videoExtractor.release();
        appendVideoExtractor.release();
        mediaMuxer.release();
    }



    @SuppressLint("WrongConstant")
    private void copyToMuxer(MediaExtractor mediaExtractor, MediaMuxer mediaMuxer, int extractorIndex, int muxerIndex,long appendTimes) {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocateDirect(500 * 1024);
        int sampleSize = 0;
        mediaExtractor.selectTrack(extractorIndex);
        while ((sampleSize = mediaExtractor.readSampleData(buffer, 0)) >= 0) {
            info.size = sampleSize;
            info.presentationTimeUs = mediaExtractor.getSampleTime() + appendTimes;
            info.flags = mediaExtractor.getSampleFlags();
            info.offset = 0;
            mediaMuxer.writeSampleData(muxerIndex, buffer, info);
            mediaExtractor.advance();
        }
        mediaExtractor.unselectTrack(extractorIndex);
    }

    private int findTrack(MediaExtractor mediaExtractor, boolean audio) {
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            String mime = mediaExtractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (audio) {
                if (mime.contains("audio/")) {
                    return i;
                }
            } else {
                if (mime.contains("video/")) {
                    return i;
                }
            }
        }
        return -1;
    }
}
