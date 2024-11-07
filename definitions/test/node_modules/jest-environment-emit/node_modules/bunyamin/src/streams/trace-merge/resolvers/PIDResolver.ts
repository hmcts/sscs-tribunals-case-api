import { RootNode } from './tree';
import type { Resolver } from './Resolver';

export class PIDResolver implements Resolver {
  protected tree = new RootNode();

  add(pid: number, _filePath: string, tid: number) {
    this.tree.addPID(pid).addFile('').addTID(tid);
  }

  finalize() {
    this.tree.rank();
  }

  resolvePid(_filePath: string, pid: number): number {
    return pid;
  }

  resolveTid(_filePath: string, pid: number, tid: number): number {
    const $pid = this.tree.findByValue(pid);
    const $file = $pid?.findByValue('');
    return $file?.transpose(tid) ?? Number.NaN;
  }
}
