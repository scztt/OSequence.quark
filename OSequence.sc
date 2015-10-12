OSequence {
	var <events;
	var <>extend=false, <>duration = 0;

	*new {
		|...args|
		var obj = super.new.init;
		args.pairsDo {
			|time, value|
			obj.put(time, value)
		};
		^obj
	}

	init {
		events = Order();
	}

	do {
		|func|
		events.do {
			|eventList, t|
			eventList.do {
				|event|
				func.value(event, t);
			}
		}
	}

	put {
		|time, value|
		if (value.notNil) {
			var eventList = events[time] ?? {
				var newList;
				events[time] = newList = List();
				newList;
			};
			if (extend && (time > duration)) {
				duration = time;
			};
			eventList.add(value);
		}
	}

	putAll {
		|time, valueArray|
		if (valueArray.notNil) {
			var eventList = events[time] ?? {
				var newList;
				events[time] = newList = List();
				newList;
			};
			if (extend && (time > duration)) {
				duration = time;
			};
			eventList.addAll(valueArray);
		}
	}

	putSeq {
		|baseTime, seq|
		if (seq.notNil) {
			seq.events.do {
				|events, time|
				this.putAll(baseTime + time, events)
			};
		}
	}

	delete {
		|start=0, end, ripple=false|
		end = end ?? duration;
		events = events.reject({
			|val, t|
			(t >= start) && (t <= end)
		});

		if (ripple) {
			var range = end - start;
			this.warp({
				|t|
				if (t > end) { t - range } { t };
			})
		};
	}

	crop {
		|start=0, end|
		var newEvents = Order();
		end = end ?? duration;
		events.do({
			|val, t|
			if ((t >= start) && (t <= end)) {
				newEvents[t - start] = val;
			}
		});
		events = newEvents;
		duration = end - start;
	}

	envWarp {
		|env|
		env = env.copy.duration_(duration);
		this.warp({
			|time|
			env.at(time);
		})
	}

	warp {
		|func|
		var oldEvents = events;
		events = Order();
		oldEvents.do({
			|eventList, time|
			eventList.do {
				|event|
				this.put(func.value(time, event), event);
			}
		});
	}

	stretch {
		|newDuration|
		var ratio = newDuration / duration;
		this.warp({ |t| t * ratio });
		duration = newDuration;
	}

	stretchBy {
		|factor=1|
		this.warp({ |t| t * factor });
		duration = duration * factor;
	}

	reverse {
		this.warp({ |t| duration - t })
	}

	doPutSeq {
		|func|
		events.copy.do {
			|eventList, time|
			eventList.do {
				|event|
				this.putSeq(time, func.value(event, time))
			}
		}
	}

	doReplace {
		|func|
		events = events.collect({
			|eventList, time|
			eventList.collect({
				|event|
				func.value(event, time)
			}).reject(_.isNil)
		})
	}

	doReplaceSeq {
		|func|
		var oldEvents = events;
		events = Order();
		oldEvents.do {
			|eventList, time|
			eventList.do {
				|event|
				this.putSeq(time, func.value(event, time));
			};
		}
	}

	sub {
		|start = 0, end|
		var sub = this.copy;
		sub.crop(start, end);
		^sub;
	}

	overwrite {
		|start, seq|
		var end = start + seq.duration;
		this.delete(start, end);
		this.putSeq(start, seq);
	}

	insert {
		|start, end, seq|
		var insertDur = end - start;
		this.delete(start, end);
		if (insertDur != seq.duration) {
			this.warp({
				|t|
				if (t > end) {
					t = t + duration - insertDur
				} {
					t
				}
			})
		};
		this.putSeq(start, seq);
	}

	replaceSub {
		|start=0, end, func, ripple=false|
		var sub;

		if (func.isKindOf(OSequence)) {
			sub = func;
		} {
			sub = this.sub(start, end);
			sub = func.value(sub);
		};
		if (ripple) {
			this.insert(start, end, sub)
		} {
			this.overwrite(start, sub);
		}
	}
}