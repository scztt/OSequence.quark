Psub : ListPattern {
	var <>rebuild;

	*new {
		|list, repeats=inf, rebuild=false|
		^super.newCopyArgs(list, repeats, rebuild)
	}

	asStream {
		^Subdivision.asDeltaStream(list, rebuild, repeats)
	}
}

Psubv : ListPattern {
	var <>rebuild;

	*new {
		|list, repeats=inf, rebuild=false|
		^super.newCopyArgs(list, repeats, rebuild)
	}

	asStream {
		^Subdivision.asValueStream(list, rebuild, repeats)
	}
}

Subdivision : Operand {
	var <>scale, <>offset, <>isRest, <>grid;

	*new {
		|value, scale=1, offset=0, grid=nil, isRest=false|
		^(super.new(value)
			.scale_(scale)
			.offset_(offset)
			.grid_(grid)
			.isRest_(isRest));
	}

	copy {
		^Subdivision(
			value, scale, offset, grid, isRest
		)
	}

	g {
		|value=0.125|
		grid = value
	}

	asSubdivision {
		^this
	}

	isArray {
		^value.isArray
	}

	|+ {
		|value|
		offset = offset + value
	}

	|- {
		|value|
		offset = offset - value
	}

	|* {
		|scalar|
		scale = scale * scalar;
	}

	|/ {
		|scalar|
		scale = scale / scalar;
	}

	valueArray {
		^this.value
	}

	duration {
		^scale
	}

	collect {
		|function|
		^this.copy.value_(
			value.collect(function)
		).isRest_(false)
	}

	deepCollect {
		arg depth = 1, function, index = 0, rank = 0;
		^this.copy.value_(
			value.deepCollect(depth, function, index, rank);
		)
	}

	*render {
		|subdivisions, length=2, durations=true, doGrid=true|
		var total, result;
		var seq, time;

		subdivisions = subdivisions.asArray;
		subdivisions = subdivisions.collect({
			|s|
			if (durations and: s.isNumber) {
				().asSubdivision.scale_(s)
			} {
				s.asSubdivision;
			}
		});

		total = subdivisions.collect(_.duration).sum;

		time = 0;
		seq = OSequence();

		subdivisions.do {
			|subdiv, i|
			var duration, offset;

			duration = subdiv.duration / total;
			offset = subdiv.offset / total;

			if (subdiv.isRest.not) {
				seq.put(time + offset, (
					dur:		duration,
					sustain: 	duration,
					subdiv: 	subdiv,
					grid:		subdiv.grid,
					value:      subdiv.value
				));
			};

			time = time + duration;
		};
		seq.duration = 1.0;

		seq.doReplaceSeq({
			|event, time|
			var seq;

			if (event[\subdiv].isArray) {
				seq = this.render(
					event[\subdiv].value,
					event[\sustain],
					durations,
					false
				);

				seq.do({
					|e|
					e[\grid] = e[\grid] ?? { event[\grid] }
				});

				seq;
			} {
				OSequence(0, event);
			}
		});

		seq.stretch(length);

		if (doGrid) {
			seq.warp({
				|time, event|
				if (event[\grid].notNil) {
					time.round(event[\grid])
				} {
					time
				}
			}, false);
		};

		seq.do({
			|e|
			e[\dur] = nil;
			e[\grid] = nil;
		});

		^seq;
	}

	*asValueStream {
		|desc, repeats=inf, rerender=false|
		var seq;

		^Plazy({
			|e|

			e.use {
				if (seq.isNil || rerender) {
					seq = desc.deepCollect(nil, {
						|v|
						v.value
					});

					seq = this.render(seq, e[\loop] ? 2, false);
				};

				seq.asValueStream(repeats);
			}
		}).asStream

	}

	*asDeltaStream {
		|desc, repeats=inf, rerender=false|
		var seq;

		^Plazy({
			|e|

			e.use {
				if (seq.isNil || rerender) {
					seq = desc.deepCollect(nil, {
						|v|
						v.value
					});

					seq = this.render(seq, e[\loop] ? 2, true);
				};

				seq.asDeltaStream(repeats);
			}
		}).asStream
	}
}

+Object {
	asSubdivision {
		^Subdivision(this).isRest_(this.isRest)
	}
}

+SimpleNumber {
	asSubdivision {
		^Subdivision(this)
	}

	|+ {
		|value|
		^(this.asSubdivision |+ value)
	}

	|- {
		|value|
		^(this.asSubdivision |- value)
	}

	|* {
		|value|
		^(this.asSubdivision |* value)
	}

	|/ {
		|value|
		^(this.asSubdivision |/ value)
	}

	g {
		|value|
		^this.asSubdivision.g(value)
	}
}

+Pattern {
	asSubdivision {
		^this.asStraem.asSubdivision
	}
}

+Object {
	asSubdivision {
		^Subdivision(this).isRest_(this.isRest)
	}

	|+ {
		|value|
		^(this.asSubdivision |+ value)
	}

	|- {
		|value|
		^(this.asSubdivision |- value)
	}

	|* {
		|value|
		^(this.asSubdivision |* value)
	}

	|/ {
		|value|
		^(this.asSubdivision |/ value)
	}

	g {
		|value|
		^this.asSubdivision.g(value)
	}
}

+Symbol {
	isRest {
		^(this == \rest) || (this == \)
	}
}



