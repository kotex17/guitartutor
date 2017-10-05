package guitartutorandanalyser.guitartutor;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

public class Tutor extends AppCompatActivity {


    final int SAMPLE_RATE = 44100;

    final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT);

    AudioRecord recorder;

    boolean isSoundRecording;
    boolean isSoundPlaying;
    Button recButton;

    String PATH_NAME = Environment.getExternalStorageDirectory() + "/GuitarTutorRec";
    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor);

        recButton = (Button) findViewById(R.id.button_play_stop);
    }

    public void onButtonPlayClick(View v) {

        if (isSoundPlaying) {
            stopPlay();
        } else {
            startPlay();
        }
    }

    public void onButtonAnalyseClick(View v) {
        analyseRecord();
    }

    public void onButtonRecClick(View v) {
        startRecording();
    }

    public void onButtonStopClick(View v) {
        stopRecording();
    }


    private void stopPlay() {

        mediaPlayer.stop();
        mediaPlayer.release();
        isSoundPlaying = false;
        recButton.setText("Play");

    }

    private void startPlay() {

        mediaPlayer = MediaPlayer.create(this, R.raw.testacd);
        mediaPlayer.start();
        isSoundPlaying = true;
        recButton.setText("Stop");

    }

    private void stopRecording() {

        if (recorder != null) {

            recorder.stop();
            recorder.release();
            recorder = null;
            isSoundRecording = false;
        }

       // createWaveFromAudioRecord();
    }

  /*  private void createWaveFromAudioRecord() {

        File recordedAudio = new File(PATH_NAME);

        try {
            FileInputStream is = new FileInputStream(PATH_NAME);
            is.read();

            FileOutputStream os = new FileOutputStream(PATH_NAME + ".wav");
            os.write(createWavHeader(recordedAudio.length()));


        } catch (Exception e) {

        }
    }*/

    private void startRecording() {

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

        recorder.startRecording();
        isSoundRecording = true;

        Thread recordAudioThread = new Thread(new Runnable() {

            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordAudioThread.start();
    }

    private void writeAudioDataToFile() {
        // Read audio data into short buffer, Write the output audio in byte

        short sData[] = new short[BUFFER_SIZE];

        try {
            FileOutputStream os = new FileOutputStream(PATH_NAME);

            while (isSoundRecording) {
                // read audio from microphone, short format
                recorder.read(sData, 0, BUFFER_SIZE);

                // writes audio data into file from buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BUFFER_SIZE * 2);
            }

            os.close();
        } catch (IOException e) {
            Log.d("Audio record error", e.getMessage().toString());
        }
    }

    private byte[] short2byte(short[] sData) {
        // all android supports little endianness, Android ARM systems are bi endian, by deafault littleendian (manualy can be swithced to big endian)
        //android audio format PCM16bit record in the default device native endian
        byte[] bytes = new byte[sData.length * 2];

        for (int i = 0; i < sData.length; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF); //hexadecimal 00 FF = 0000 0000 1111 1111 masking
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8); // shift 8 right,
        }
        return bytes;
    }

    private byte[] createWavHeader(long recordedAudioLength) {
        // The default byte ordering assumed for WAVE data files is little-endian. Files written using the big-endian byte ordering scheme have the identifier RIFX instead of RIFF.

        short channels = 1; //mono
        short bitsPerSample = 16; // pcm 16bit

        int subchunk2Size = (int) recordedAudioLength; // int enough, recorded audio is max a few minutes
        int chunkSize = (int) (recordedAudioLength + 36); //wave file header is 44 bytes, first 4 is ChunkId, second 4 bytes are ChunkSize: 44 - 8 = 36 bytes

        byte[] headerBytes = ByteBuffer
                .allocate(22)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(chunkSize)
                .putShort(channels)
                .putInt(SAMPLE_RATE)
                .putInt(SAMPLE_RATE * channels * (bitsPerSample / 8))
                .putShort((short) (channels * (bitsPerSample / 8)))
                .putShort(bitsPerSample)
                .putInt((subchunk2Size))
                .array();

        byte[] header = new byte[]{
                'R', 'I', 'F', 'F', // ChunkID
                headerBytes[0], headerBytes[1], headerBytes[2], headerBytes[3], // ChunkSize
                'W', 'A', 'V', 'E', // Format
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // audioformat: 1=PCM
                headerBytes[4], headerBytes[5], // No. of channels 1= mono, 2 = stereo
                headerBytes[6], headerBytes[7], headerBytes[8], headerBytes[9], // SampleRate
                headerBytes[10], headerBytes[11], headerBytes[12], headerBytes[13], // ByteRate
                headerBytes[14], headerBytes[15], // BlockAlign
                headerBytes[16], headerBytes[17], // BitsPerSample
                'd', 'a', 't', 'a', // Subchunk2ID
                headerBytes[18], headerBytes[19], headerBytes[20], headerBytes[21] // Subchunk2Size
        };

        return header;
    }

    private void analyseRecord() {
        Log.d("started analyse", "started analysing");
        new AndroidFFMPEGLocator(this);

        int SAMPLE_RATE = 44100;

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);


        AudioRecord record =
                new AudioRecord(MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(PATH_NAME, SAMPLE_RATE, bufferSize / 2, 0);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.DYNAMIC_WAVELET, SAMPLE_RATE, bufferSize / 2, new PitchDetectionHandler() {

            @Override
            public void handlePitch(PitchDetectionResult pitchDetectionResult,
                                    AudioEvent audioEvent) {
                final float pitchInHz = pitchDetectionResult.getPitch();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("pitch detected: ", String.valueOf(pitchInHz));
                    }
                });

            }
        }));
        new Thread(dispatcher, "Audio Dispatcher").start();
    }
}
