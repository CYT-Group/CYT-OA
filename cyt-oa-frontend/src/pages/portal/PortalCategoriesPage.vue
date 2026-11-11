<template>
  <div class="min-w-0 space-y-4">
    <PageHeader title="分类管理" description="维护培训大类、小类和物品品类，支持排序、归档与恢复。" />
    <SegmentedTabs v-if="options.length" :model-value="scope" :items="options" @update:model-value="selectScope" />
    <PortalCategoryPanel v-if="scope" :key="scope" :scope="scope" :title="options.find(item => item.value === scope)?.label" />
    <Callout v-else tone="destructive" title="没有分类管理权限">请联系管理员分配培训或物品管理权限。</Callout>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { session } from '../../lib/session'
import PageHeader from '../../ui/PageHeader.vue'
import SegmentedTabs from '../../ui/SegmentedTabs.vue'
import Callout from '../../ui/Callout.vue'
import PortalCategoryPanel from '../../components/portal/PortalCategoryPanel.vue'
const route = useRoute(), router = useRouter()
const options = computed(() => [
  ...(session.has('can_manage_training') ? [{ value: 'training.course', label: '培训分类' }] : []),
  ...(session.has('can_manage_materials') ? [{ value: 'material.sample', label: '样品品类' }, { value: 'material.office', label: '办公用品品类' }] : [])
])
const scope = computed(() => options.value.find(x => x.value === route.query.scope)?.value ?? options.value[0]?.value ?? '')
function selectScope(value: string | number) { void router.replace({ query: { ...route.query, scope: String(value) } }) }
</script>
