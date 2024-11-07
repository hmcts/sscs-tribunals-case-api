/* eslint-disable prefer-const */
import { Bunyamin } from './decorator';
import { noopLogger } from './noopLogger';
import { isSelfDebug } from './is-debug';
import { ThreadGroups } from './thread-groups';

type Realm = {
  bunyamin: Bunyamin;
  nobunyamin: Bunyamin;
  threadGroups: ThreadGroups;
};

function create() {
  let bunyamin: Bunyamin;
  let nobunyamin: Bunyamin;

  const selfDebug = isSelfDebug();
  const threadGroups = new ThreadGroups(() => bunyamin);

  bunyamin = new Bunyamin({
    logger: noopLogger(),
    threadGroups,
  });

  nobunyamin = new Bunyamin({
    immutable: true,
    logger: noopLogger(),
    threadGroups,
  });

  if (selfDebug) {
    bunyamin.trace({ cat: 'bunyamin' }, 'bunyamin global instance created');
  }

  return { bunyamin, nobunyamin, threadGroups };
}

function getCached(): Realm | undefined {
  const result = (globalThis as any).__BUNYAMIN__;

  if (isSelfDebug() && result) {
    result.bunyamin.trace({ cat: 'bunyamin' }, 'bunyamin global instance retrieved from cache');
  }

  return result;
}

function setCached(realm: Realm) {
  (globalThis as any).__BUNYAMIN__ = realm;
  return realm;
}

export default setCached(getCached() ?? create());
