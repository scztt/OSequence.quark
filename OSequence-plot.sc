+OSequence {
	plot {
		|yKey=\midinote, ySpec=nil, plotType=\event|
		var view, keys, minVal, maxVal, xSpec, scale;
		var sustainFunc = {
			|event|
			event = event.copy;
			event.parent_(Event.parentEvents.default)
				.use({
					event[\sustain].value(event);
				})
		};

		var valueFunc = {
			|event|
			event = event.copy;
			event.parent_(Event.parentEvents.default)
				.use({
					event[yKey].value(event);
				})
		};

		scale = Scale.majorPentatonic;

		ySpec = ySpec ?? { ControlSpec(0, 1) };

		keys = Set();
		this.do {
			|event, time|
			event = event.asPairs.flop.collect(_.asEvent);
			event.do {
				|event|
				keys.add(valueFunc.(event))
			}
		};

		keys = SortedList.newFrom(keys);
		minVal = ((keys.first / 12).floor * 12).round(1);
		maxVal = ((keys.last / 12).ceil * 12).round(1);

		ySpec.minval = minVal;
		ySpec.maxval = maxVal+1;

		xSpec = ControlSpec(
			0, events.indices.maxValue({ |v| v })
		);

		view = UserView();
		view.drawFunc = {
			|v|
			var colors = [Color.white, Color.red];
			var bounds = v.bounds.moveTo(0,0);
			var max = ySpec.maxval;
			var min = ySpec.minval;
			var xPos, yPos;
			var xScale = 16;
			var yScale = 8;
			var height = (max - min) * yScale;

			// Background - y axis
			(min..max).doAdjacentPairs {
				|a, b, i|
				var scaledA, scaledB;

				scaledA = 1 - ySpec.unmap(a);
				scaledB = 1 - ySpec.unmap(b);

				Pen.fillColor = [
					Color.grey(0.6, 0.4),
					Color.grey(0.5, 0.4)
				][
					[0, 1, 0, 1, 0, 1, 0,
						0, 1, 0, 1, 0
					].wrapAt(a + 8)
				];
				Pen.fillRect(Rect(
					0,            scaledA * height,
					bounds.width, (scaledB - scaledA) * height
				));
			};

			// Background - x axis
			xPos = 0;
			while { xPos < bounds.width } {
				if (0 == (xPos % (xScale * 4))) {
					Pen.lineDash = FloatArray.newFrom([1, 0]);
					Pen.strokeColor = Color.green(0.8, 0.4);
				} {
					Pen.lineDash = FloatArray.newFrom([4.0, 2.0]);
					Pen.strokeColor = Color.grey(1, 0.1)
				};
				Pen.width = 1;
				Pen.line(xPos@0, xPos@bounds.height);
				xPos = xPos + xScale;
				Pen.stroke;
			};

			// Notes
			if (plotType == \event) {
				this.do {
					|event, time|
					event = event.asPairs.flop.collect(_.asEvent);
					event.do {
						|event|
						var value, sustain;

						sustain = sustainFunc.(event);
						value = ySpec.postln.unmap(valueFunc.(event).postln);

						xPos = time * xScale;
						yPos = (1 - value) * height;
						[value, sustain, xPos, yPos].postln;

						Pen.fillColor = Color.hsv(0.6, 0.6, 1);
						Pen.strokeColor = Color.hsv(0.6, 0.6, 1);
						Pen.lineDash = FloatArray.newFrom([1, 0]);
						Pen.width = 4;
						Pen.capStyle = 2;
						Pen.fillRect(
							Rect(
								xPos, yPos,
								yScale, yScale
							)
						);
						Pen.line(
							(xPos + yScale) @ (yPos + (yScale / 2)),
							(xPos + yScale + (sustain * xScale)) @ (yPos + (yScale / 2)),
						);
						Pen.stroke;
					}
				}
			}
		};

		view.fixedSize = 2000@600;

		ScrollView(bounds:600@300).canvas_(view).front;
	}
}