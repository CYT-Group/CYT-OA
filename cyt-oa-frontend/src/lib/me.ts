import { reactive, ref } from "vue";
import { api } from "./api";
import { session } from "./session";

export interface MeSummary {
  deptName: string;
  jobTitle: string;
  /** 有直属下属才显示「我的团队」，后端按花名册 manager 关系算出来 */
  teamCount: number;
  counts: { reminder: number; report: number; team: number };
}

const empty: MeSummary = {
  deptName: "",
  jobTitle: "",
  teamCount: 0,
  counts: { reminder: 0, report: 0, team: 0 },
};

export const meSummary = reactive<MeSummary>({ ...empty });
export const meSummaryLoaded = ref(false);

let loading: Promise<void> | null = null;
let generation = 0;

export function resetMeSummary() {
  generation++;
  loading = null;
  meSummaryLoaded.value = false;
  Object.assign(meSummary, { ...empty, counts: { ...empty.counts } });
}

export function loadMeSummary(force = false): Promise<void> {
  if (loading && !force) return loading;
  const owner = session.token;
  const requestGeneration = generation;
  const pending = api
    .get<MeSummary>("/api/me/summary")
    .then((data) => {
      if (!data || owner !== session.token || requestGeneration !== generation)
        return;
      meSummary.deptName = data.deptName ?? "";
      meSummary.jobTitle = data.jobTitle ?? "";
      meSummary.teamCount = data.teamCount ?? 0;
      meSummary.counts = { ...empty.counts, ...(data.counts ?? {}) };
      meSummaryLoaded.value = true;
    })
    .catch(() => {
      if (owner !== session.token || requestGeneration !== generation) return;
      meSummaryLoaded.value = false;
      Object.assign(meSummary, JSON.parse(JSON.stringify(empty)));
    })
    .finally(() => {
      if (loading === pending) loading = null;
    });
  loading = pending;
  return pending;
}
