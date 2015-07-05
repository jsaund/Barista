Barista
=======

Barista is a suite of Android widgets. The suite is currenlty comprised of:
* Audio Visualizer
* Radial Progress Bar Indicator

###Audio Visualizer
The audio visualizer widget bridges the gap between audio capture and audio visualization. The widget translates captured audio samples from the time domain to the frequency domain. A histogram of frequency magnitudes is then provided to the developer to visualize the audio spectrum in real time.
Two renders are provided as examples of how to use the widget:
* BarRender
* RoundedBarRender

###Radial Progress Bar Indicator
The Radial Progress Indicator provides a visual indicator of progress for an operation. The progress is displayed as a radial arc from [0 .. 360].
The component operates in 3 modes:
* Timer: The application can provide a timeout duration in seconds. The radial indicator will countdown from the duration to 0. A callback is triggered upon reaching 0.
* Percentage: The application can update the amount of progress and the indicator will advance the position radially along the track of the indicator. The overall progress is displayed as a percentage from [0 .. 100] in the center of the radial indicator.
* Fixed: Operates similar to Percentage with the exception that the progress value is rendered as the current value. For example: if the max progress is set to 3000 and the current progress is 500 then 500 will be displayed in the center (as opposed to the percentage).
