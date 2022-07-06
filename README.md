(Folked from: https://github.com/ditek/MidiSheetMusic-Android)

# MidiSheetMusic-android (and recognition piano music sheet)
I used [MidiSheetMusic-Android from ditek](https://github.com/ditek/MidiSheetMusic-Android) and [Onsets and Frames: Realtime TFLite Demo from magenta](https://github.com/magenta/magenta/tree/main/magenta/models/onsets_frames_transcription/realtime).
I also refer method to convert Onset and Frames into Keras to reduce model size from [PolyphonicPianoTranscription](https://github.com/BShakhovsky/PolyphonicPianoTranscription).

The project is in progress. My models is very slow and not accurate. Please give me some comments and suggestion.

An app for visualizing MIDI files with extra features to make learning new pieces easier.

<div style="text-align: center">

<img src="images/screenshot_sheet.png" width="500" hspace="20">
<br>
<img src="images/screenshot_song_list.png" width="500" hspace="20">
<br>
<img src="images/screenshot_settings1.png" height="500" hspace="20"><img src="images/screenshot_settings2.png" height="500" hspace="20">

</div>

This project has originally been created by Madhav Vaidy. However, it seems to have been abandoned since 2013. When I started using the app I got frustrated by some of the bugs. That's why I deceided to take over development.
The original project can be found [here](https://sourceforge.net/projects/midisheetmusic).

So far, the app has been updated to work with the latest version of Android, numerous bugs have been fixed and several features have been introduced. The app has been visually redesigned as well.

The next big feature in the works is adding external device support. That would allow a digital piano or keyboard to be connected to the phone/table and the app would give proper real-time feedback. I plan to publish the app to the Play Store after that. And, of course, it will still be free.

Please feel free to open a ticket for reporting a bug or requesting a feature.

## Contributions
- Thanks to [ankineri](https://github.com/ankineri) for implementing MIDI keyboard support.
