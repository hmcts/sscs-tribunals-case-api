import type { JestEnvironment } from '@jest/environment';
import type { EnvironmentListenerOnly } from '../types';
export declare function requireModule<E extends JestEnvironment = JestEnvironment>(rootDir: string, moduleName: string): Promise<EnvironmentListenerOnly<E> | null>;
