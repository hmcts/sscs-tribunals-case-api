import { PIDResolver } from './PIDResolver';

export class FilePIDResolver extends PIDResolver {
  add(pid: number, filePath: string, tid: number) {
    this.tree.addPID(pid).addFile(filePath).addTID(tid);
  }

  resolvePid(filePath: string, pid: number): number {
    const $pid = this.tree.findByValue(pid);
    const $file = $pid?.findByValue(filePath);
    return ($file?.rank ?? Number.NaN) + 1;
  }

  resolveTid(filePath: string, pid: number, tid: number): number {
    const $pid = this.tree.findByValue(pid);
    const $file = $pid?.findByValue(filePath);
    return $file?.transpose(tid) ?? Number.NaN;
  }
}
