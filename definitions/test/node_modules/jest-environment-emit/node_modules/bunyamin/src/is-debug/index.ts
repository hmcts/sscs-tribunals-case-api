import { createIsDebug } from './createIsDebug';

export const isDebug = createIsDebug(process.env.DEBUG || '');

export const isSelfDebug = () => isDebug('bunyamin');
