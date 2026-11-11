import { reactive } from 'vue';
import { session } from './session';
import { workspaceApi, type WorkspaceContext } from './workspace-api';

export const workspaceContext = reactive<{ owner: string; value: (WorkspaceContext & { canApproveMaterials?: boolean }) | null }>({ owner: '', value: null });
let currentRequest = 0;
export async function loadWorkspaceContext() {
  const owner = session.token, request = ++currentRequest;
  if (workspaceContext.owner !== owner) workspaceContext.value = null;
  workspaceContext.owner = owner;
  if (!owner || session.isHrOnly) return;
  try { const value = await workspaceApi.context(); if (request === currentRequest && owner === session.token) workspaceContext.value = value; }
  catch { if (request === currentRequest && owner === session.token) workspaceContext.value = null; }
}
