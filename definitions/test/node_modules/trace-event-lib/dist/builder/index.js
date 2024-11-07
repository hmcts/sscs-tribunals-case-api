"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AbstractEventBuilder = void 0;
const utils_1 = require("../utils");
class AbstractEventBuilder {
    begin(event) {
        const { args, tts, ts, cat, name, sf, cname, pid, tid, stack } = this.defaults(event);
        this.event({
            ph: 'B',
            args,
            tts,
            ts,
            cat,
            name,
            sf,
            cname,
            pid,
            tid,
            stack,
        });
    }
    beginAsync(event) {
        const { args, cat, name, pid, tid, ts, tts, cname, id, id2, scope } = this.defaults(event);
        this.event({
            ph: 'b',
            args,
            cat,
            name,
            pid,
            tid,
            ts,
            tts,
            cname,
            id,
            id2,
            scope,
        });
    }
    complete(event) {
        const { tid, pid, ts, args, tts, sf, cname, dur, stack, esf, cat, name, tdur, estack } = this.defaults(event);
        this.event({
            ph: 'X',
            args,
            cat,
            cname,
            dur,
            esf,
            estack,
            name,
            pid,
            sf,
            stack,
            tdur,
            tid,
            ts,
            tts,
        });
    }
    counter(event) {
        const { args, cat, cname, pid, tid, ts, tts, name, id } = this.defaults(event);
        this.event({ ph: 'C', args, cat, cname, pid, tid, ts, tts, name, id });
    }
    end(event = {}) {
        const { args, tts, ts, sf, cname, pid, tid, stack } = this.defaults(event);
        this.event({ ph: 'E', args, tts, ts, sf, cname, pid, tid, stack });
    }
    endAsync(event) {
        const { args, tts, tid, ts, pid, cname, id, id2, scope, cat, name } = this.defaults(event);
        this.event({
            ph: 'e',
            args,
            tts,
            tid,
            ts,
            pid,
            cname,
            id,
            id2,
            scope,
            cat,
            name,
        });
    }
    instant(event) {
        const { args, sf, cname, pid, tid, ts, tts, cat, name, s, stack } = this.defaults(event);
        this.event({
            ph: 'i',
            args,
            cat,
            cname,
            name,
            pid,
            s,
            sf,
            stack,
            tid,
            ts,
            tts,
        });
    }
    instantAsync(event) {
        const { args, cat, name, pid, tid, ts, tts, cname, id, id2, scope } = this.defaults(event);
        this.event({
            ph: 'n',
            args,
            cat,
            name,
            pid,
            tid,
            ts,
            tts,
            cname,
            id,
            id2,
            scope,
        });
    }
    metadata(event) {
        const { args, tts, ts, tid, pid, cname, cat, name } = this.defaults(event);
        this.event({ ph: 'M', args, tts, ts, tid, pid, cname, cat, name });
    }
    process_labels(labels, pid) {
        this.metadata({
            pid,
            name: 'process_labels',
            args: { labels: labels.join(',') },
        });
    }
    process_name(name, pid) {
        this.metadata({
            pid,
            name: 'process_name',
            args: { name },
        });
    }
    process_sort_index(index, pid) {
        this.metadata({
            pid,
            name: 'process_sort_index',
            args: { sort_index: index },
        });
    }
    thread_name(name, tid, pid) {
        this.metadata({
            pid,
            tid,
            name: 'thread_name',
            args: { name },
        });
    }
    thread_sort_index(index, tid, pid) {
        this.metadata({
            pid,
            tid,
            name: 'thread_sort_index',
            args: { sort_index: index },
        });
    }
    event(event) {
        this.send((0, utils_1.compactObject)(this.defaults(event)));
    }
    defaults(event) {
        const { ts = (0, utils_1.now)(), pid = (0, utils_1.getProcessId)(), tid = 0 } = event;
        return {
            ...event,
            ts,
            pid,
            tid,
        }; // eslint-disable-line @typescript-eslint/no-explicit-any
    }
}
exports.AbstractEventBuilder = AbstractEventBuilder;
//# sourceMappingURL=index.js.map