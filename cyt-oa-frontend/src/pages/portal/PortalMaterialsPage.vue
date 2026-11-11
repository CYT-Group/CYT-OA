<template>
  <div>
    <PageHeader
      :title="isOffice ? '办公用品管理' : '样品库管理'"
      :description="
        isOffice
          ? '维护办公用品商品、规格库存、领用单据和站点负责人。'
          : '维护样品商品、规格库存与借用归还记录。'
      "
      ><template #actions
        ><Button variant="outline" :disabled="loading" @click="reload"
          ><RefreshCw class="size-4" />刷新</Button
        ><Button
          v-if="tab === 'products' || tab === 'inventory'"
          :disabled="optionsLoading"
          @click="openEdit()"
          ><Plus class="size-4" />{{
            tab === "products" ? "新建商品" : "新增规格"
          }}</Button
        ><Button
          v-if="tab === 'applies'"
          :disabled="optionsLoading"
          @click="openCreateApply"
          ><Plus class="size-4" />{{
            isOffice ? "登记领用" : "登记借用"
          }}</Button
        ></template
      ></PageHeader
    >
    <SegmentedTabs
      class="mt-4"
      :model-value="tab"
      :items="tabs"
      @update:model-value="changeTab"
    />
    <section v-if="tab === 'inventory'" class="mt-4" aria-label="全局库存概况">
      <div class="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 class="text-sm font-semibold">库存概况</h2>
        <p class="text-xs text-muted-foreground">{{ isOffice ? '办公用品' : '样品' }}{{ summaryArchived ? '已归档' : '正常' }}台账，不受下方筛选影响</p>
      </div>
      <div class="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <div v-for="metric in stockMetrics" :key="metric.label" class="panel p-4">
          <p class="text-xs text-muted-foreground">{{ metric.label }}</p>
          <p class="mt-2 text-2xl font-semibold tabular-nums">{{ loading ? '…' : (metric.value ?? '—') }}</p>
        </div>
      </div>
    </section>
    <Callout
      v-if="optionsError"
      title="基础信息加载失败"
      tone="destructive"
      class="mt-4"
      >{{ optionsError
      }}<template #action
        ><Button size="sm" variant="outline" @click="loadOptions"
          >重试</Button
        ></template
      ></Callout
    >
    <section v-if="tab === 'owners'" class="panel mt-4 p-4 sm:p-5">
      <h2 class="text-sm font-semibold">站点负责人</h2>
      <p class="mb-5 mt-2 text-xs text-muted-foreground">
        各站点负责人处理本地物资申请，修改后新申请按新的负责人分派。
      </p>
      <div v-if="optionsLoading" class="py-8 text-sm text-muted-foreground">
        正在加载负责人…
      </div>
      <div v-else class="grid gap-4 md:grid-cols-2">
        <div
          v-for="site in sites"
          :key="site.value"
          class="rounded-lg border p-4"
        >
          <h3 class="mb-2 font-medium">{{ site.label }}</h3>
          <p class="mb-4 text-xs text-muted-foreground">
            当前负责人：{{ ownerOf(site.value)?.ownerName || "尚未设置" }}
          </p>
          <label :for="`owner-${site.value}`" class="field-label">负责人</label
          ><Select
            :id="`owner-${site.value}`"
            v-model="ownerDraft[site.value]"
            :options="userOptions"
            placeholder="选择员工"
            :disabled="!canConfigureOwners || busy"
          /><Button
            v-if="canConfigureOwners"
            class="mt-3"
            size="sm"
            :disabled="!ownerDraft[site.value] || busy"
            @click="saveOwner(site.value)"
            >保存负责人</Button
          >
        </div>
      </div>
    </section>
    <section v-else class="panel mt-4 overflow-hidden">
      <form
        class="table-toolbar flex flex-wrap gap-2"
        @submit.prevent="applyFilters"
      >
        <Input
          v-model="search"
          :placeholder="
            tab === 'applies' ? '搜索单号、申请人或用途' : '搜索商品名或品牌'
          "
          aria-label="搜索物资"
          class="sm:w-72"
        /><Select
          v-if="tab !== 'applies'"
          v-model="archiveFilter"
          :options="[
            { value: 'false', label: '正常台账' },
            { value: 'true', label: '已归档' },
          ]"
          aria-label="归档状态"
          class="sm:w-36"
        /><Select
          v-if="tab !== 'applies'"
          v-model="categoryFilter"
          :options="[{ value: '', label: '全部分类' }, ...categoryOptions]"
          aria-label="按物资分类筛选"
          class="sm:w-36"
        /><Select
          v-if="tab !== 'applies'"
          v-model="brandFilter"
          :options="brandOptions"
          aria-label="按品牌筛选"
          class="sm:w-36"
        /><Select
          v-if="tab !== 'owners'"
          v-model="siteFilter"
          :options="[{ value: '', label: '全部城市' }, ...sites]"
          aria-label="按物资城市筛选"
          class="sm:w-36"
        /><Select
          v-if="tab === 'inventory'"
          v-model="stockFilter"
          :options="[{ value: '', label: '全部库存' }, { value: 'available', label: '有可用库存' }, { value: 'out', label: '库存为零' }]"
          aria-label="按库存状态筛选"
          class="sm:w-40"
        /><Select
          v-if="tab === 'applies'"
          v-model="statusFilter"
          :options="applyStatusOptions"
          aria-label="按物资单据状态筛选"
          class="sm:w-36"
        /><Button type="submit" variant="outline" :disabled="loading"
          ><Search class="size-4" />查询</Button
        ><Button v-if="hasFilters" variant="ghost" :disabled="loading" @click="resetFilters">重置</Button>
        <span class="ml-auto self-center whitespace-nowrap text-xs text-muted-foreground">共 {{ loadError ? '—' : total }} 条</span>
      </form>
      <Callout
        v-if="loadError"
        title="台账加载失败"
        tone="destructive"
        class="m-4"
        >{{ loadError
        }}<template #action
          ><Button variant="outline" size="sm" @click="load"
            >重试</Button
          ></template
        ></Callout
      >
      <div
        v-if="loading"
        class="p-10 text-center text-sm text-muted-foreground"
      >
        正在加载台账…
      </div>
      <div v-else-if="rows.length" class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <template v-if="tab === 'products'"
                ><th>商品</th>
                <th>品牌</th>
                <th>分类</th>
                <th>规格维度</th></template
              ><template v-else-if="tab === 'inventory'"
                ><th>商品 / 规格</th>
                <th>站点</th>
                <th>SKU</th>
                <th class="num">可用库存</th>
                <th class="num">单价</th>
                <th>位置</th>
                <th>负责人</th></template
              ><template v-else
                ><th>单据编号</th>
                <th>申请人</th>
                <th>用途</th>
                <th>状态</th>
                <th>预计归还</th>
                <th>创建时间</th></template
              >
              <th class="sticky-col text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <template v-if="tab === 'products'"
                ><td>
                  <img v-if="row.imageUrls?.[0]" :src="row.imageUrls[0]" :alt="`${row.name}留底图`" class="float-left mr-3 size-10 rounded-md border object-cover" loading="lazy" />
                  <b class="portal-table-title font-medium" :title="row.name">{{
                    row.name
                  }}</b>
                  <p
                    class="mt-1 max-w-64 truncate text-xs text-muted-foreground"
                  >
                    {{ row.description }}
                  </p>
                </td>
                <td>{{ row.brand || "—" }}</td>
                <td>{{ categoryName(row.categoryId) }}</td>
                <td>
                  {{ (row.variantDims ?? []).join(" / ") || "无规格维度" }}
                </td></template
              ><template v-else-if="tab === 'inventory'"
                ><td>
                  <img v-if="row.imageUrls?.[0]" :src="row.imageUrls[0]" :alt="`${row.name}留底图`" class="float-left mr-3 size-10 rounded-md border object-cover" loading="lazy" />
                  <b class="portal-table-title font-medium" :title="row.name">{{
                    row.name
                  }}</b>
                  <p class="mt-1 text-xs text-muted-foreground">
                    {{ row.spec || "标准规格" }}
                  </p>
                  <p
                    v-if="row.currentBorrowers?.length"
                    class="mt-1 text-xs text-muted-foreground"
                  >
                    在借：{{ row.currentBorrowers.join("、") }}
                  </p>
                </td>
                <td>{{ siteName(row.site) }}</td>
                <td>
                  <span class="block max-w-40 truncate" :title="row.sku">{{
                    row.sku || "—"
                  }}</span>
                </td>
                <td class="num">{{ row.stockQty }}</td>
                <td class="num">
                  {{ row.unitPrice == null ? "—" : row.unitPrice }}
                </td>
                <td>{{ row.location || "—" }}</td>
                <td>{{ row.ownerName || "—" }}</td></template
              ><template v-else
                ><td class="font-medium">{{ row.applyNo }}</td>
                <td>{{ row.applicantName || row.applicantUsername }}</td>
                <td class="max-w-64 truncate">{{ row.purpose || "—" }}</td>
                <td>
                  <Badge
                    :variant="
                      row.status === 'rejected'
                        ? 'destructive'
                        : row.status === 'pending'
                          ? 'outline'
                          : 'secondary'
                    "
                    >{{ stateLabel(row.status) }}</Badge
                  >
                </td>
                <td>{{ row.expectReturnDate || "—" }}</td>
                <td class="whitespace-nowrap text-xs text-muted-foreground">
                  {{ time(row.createdAt) }}
                </td></template
              >
              <td class="sticky-col">
                <div class="flex items-center justify-end gap-1">
                  <Button
                    v-if="tab === 'applies'"
                    variant="ghost"
                    size="sm"
                    :disabled="busy"
                    @click="openDetail(row.id)"
                    >查看处理</Button
                  ><template v-else
                    ><Button
                      v-if="!row.archivedAt"
                      variant="ghost"
                      size="sm"
                      :disabled="busy"
                      @click="openEdit(row)"
                      >编辑</Button
                    ><Button
                      v-else
                      variant="ghost"
                      size="sm"
                      :disabled="busy"
                      @click="askRecordAction(row, 'restore')"
                      >恢复</Button
                    ><PortalRowActions
                      :disabled="busy"
                      :actions="[
                        !row.archivedAt && {
                          label: '归档记录',
                          onSelect: () => askRecordAction(row, 'archive'),
                        },
                        !!row.archivedAt &&
                          canDelete && {
                            label: '永久删除',
                            destructive: true,
                            onSelect: () => askRecordAction(row, 'delete'),
                          },
                      ]"
                  /></template>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState
        v-else-if="!loadError"
        title="暂无匹配记录"
        description="调整搜索条件，或添加新的台账记录。"
      /><Pagination
        class="border-t px-5 py-3"
        :page="page"
        :page-size="20"
        :total="total"
        :disabled="loading"
        @update:page="changePage"
      />
    </section>
  </div>

  <Dialog
    v-model:open="editOpen"
    :title="
      editResource === 'products'
        ? editId
          ? '编辑商品'
          : '新建商品'
        : editId
          ? '编辑规格库存'
          : '新增规格库存'
    "
    size="lg"
    ><Callout v-if="editError" tone="destructive" class="mb-4">{{
      editError
    }}</Callout>
    <div class="grid gap-4 sm:grid-cols-2">
      <template v-if="editResource === 'products'"
        ><div>
          <label for="material-product-name" class="field-label">商品名称</label
          ><Input
            id="material-product-name"
            v-model="productForm.name"
            maxlength="200"
          />
        </div>
        <div>
          <label for="material-brand" class="field-label">品牌</label
          ><Input
            id="material-brand"
            v-model="productForm.brand"
            maxlength="50"
          />
        </div>
        <div>
          <label for="material-category" class="field-label">分类</label
          ><Select
            id="material-category"
            v-model="productForm.categoryId"
            :options="categoryOptions"
            placeholder="选择分类"
          />
          <p
            v-if="!options.categories.length"
            class="mt-2 text-xs text-muted-foreground"
          >
            <router-link
              :to="`/portal/categories?scope=material.${kind}`"
              class="text-primary underline"
              >前往分类管理</router-link
            >，建立{{ isOffice ? "办公用品" : "样品" }}分类。
          </p>
        </div>
        <div class="sm:col-span-2">
          <label for="material-description" class="field-label">商品说明</label
          ><Textarea
            id="material-description"
            v-model="productForm.description"
            :rows="3"
          />
        </div>
        <div class="sm:col-span-2">
          <h3 class="mb-2 text-xs font-medium">规格维度（最多 8 项）</h3>
          <p class="mb-3 text-xs text-muted-foreground">
            例如颜色、尺寸、连接方式。已有规格的商品不能更改维度。
          </p>
          <div
            v-for="(_, i) in productForm.variantDims"
            :key="i"
            class="mb-2 flex items-center gap-2"
          >
            <Input
              v-model="productForm.variantDims[i]"
              :aria-label="`规格维度 ${i + 1}`"
              maxlength="32"
            /><Button
              variant="ghost"
              size="icon-sm"
              :aria-label="`删除维度 ${i + 1}`"
              @click="productForm.variantDims.splice(i, 1)"
              ><Trash2 class="size-4"
            /></Button>
          </div>
          <Button
            variant="outline"
            size="sm"
            :disabled="productForm.variantDims.length >= 8"
            @click="productForm.variantDims.push('')"
            >添加维度</Button
          >
        </div></template
      >
      <template v-else
        ><div>
          <label for="material-sku-product" class="field-label">所属商品</label
          ><Select
            id="material-sku-product"
            v-model="skuForm.productId"
            :options="productOptions"
            placeholder="选择商品"
            @update:model-value="resetVariantValues"
          />
        </div>
        <div>
          <label for="material-site" class="field-label">站点</label
          ><Select id="material-site" v-model="skuForm.site" :options="sites" />
        </div>
        <div v-for="(dim, i) in selectedProduct?.variantDims ?? []" :key="dim">
          <label :for="`material-dim-${i}`" class="field-label">{{ dim }}</label
          ><Input
            :id="`material-dim-${i}`"
            v-model="skuForm.variantValues[dim]"
            maxlength="64"
          />
        </div>
        <div>
          <label for="material-sku" class="field-label">SKU 编码</label
          ><Input id="material-sku" v-model="skuForm.sku" />
        </div>
        <div>
          <label for="material-stock" class="field-label">可用库存数量</label
          ><Input
            id="material-stock"
            v-model="skuForm.stockQty"
            type="number"
            min="0"
            step="1"
          />
        </div>
        <div>
          <label for="material-price" class="field-label">单价</label
          ><Input
            id="material-price"
            v-model="skuForm.unitPrice"
            type="number"
            min="0"
            step="0.01"
          />
        </div>
        <div>
          <label for="material-location" class="field-label">存放位置</label
          ><Input id="material-location" v-model="skuForm.location" />
        </div>
        <div class="sm:col-span-2">
          <label for="material-note" class="field-label"
            >备注 / 库存调整原因</label
          ><Textarea id="material-note" v-model="skuForm.note" :rows="2" />
        </div>
        <p
          v-if="!ownerOf(skuForm.site)"
          class="text-xs text-destructive sm:col-span-2"
        >
          请先在“站点负责人”中配置{{ siteName(skuForm.site) }}的负责人。
        </p></template
      >
      <div class="sm:col-span-2">
        <h3 class="mb-3 text-xs font-medium">图片</h3>
        <div v-for="(_, i) in editImages" :key="i" class="mb-3">
          <div class="mb-1 flex justify-end">
            <Button variant="ghost" size="sm" @click="editImages.splice(i, 1)"
              >删除图片</Button
            >
          </div>
          <PortalImageField
            v-model="editImages[i]"
            :label="`物资图片 ${i + 1}`"
            folder="materials"
          />
        </div>
        <Button variant="outline" size="sm" @click="editImages.push('')"
          >添加图片</Button
        >
      </div>
    </div>
    <template #footer
      ><Button variant="outline" :disabled="busy" @click="editOpen = false"
        >取消</Button
      ><Button :loading="busy" @click="saveRecord">保存</Button></template
    ></Dialog
  >

  <Dialog
    v-model:open="createOpen"
    :title="isOffice ? '登记办公用品领用' : '登记样品借用'"
    size="lg"
    ><Callout v-if="createError" tone="destructive" class="mb-4">{{
      createError
    }}</Callout>
    <div class="grid gap-4 sm:grid-cols-2">
      <div>
        <label for="material-applicant" class="field-label">申请人</label
        ><Select
          id="material-applicant"
          v-model="applyForm.applicantUsername"
          :options="userOptions"
          placeholder="选择申请人"
        />
      </div>
      <div v-if="!isOffice">
        <label for="material-return-date" class="field-label"
          >预计归还日期</label
        ><Input
          id="material-return-date"
          v-model="applyForm.expectReturnDate"
          type="date"
          :min="today"
        />
      </div>
      <div class="sm:col-span-2">
        <label for="material-purpose" class="field-label">用途</label
        ><Textarea
          id="material-purpose"
          v-model="applyForm.purpose"
          :rows="2"
        />
      </div>
      <div class="sm:col-span-2">
        <h3 class="mb-2 text-sm font-medium">物资明细</h3>
        <div
          v-for="(item, i) in applyForm.items"
          :key="i"
          class="mb-3 flex flex-wrap items-center gap-2"
        >
          <Select
            v-model="item.materialId"
            :options="inventoryOptions"
            :aria-label="`明细 ${i + 1} 的物资`"
            placeholder="选择规格"
            class="min-w-44 flex-1"
          /><Input
            v-model="item.quantity"
            type="number"
            min="1"
            step="1"
            :aria-label="`明细 ${i + 1} 的数量`"
            class="w-24"
          /><Button
            variant="ghost"
            size="icon-sm"
            :aria-label="`删除明细 ${i + 1}`"
            @click="applyForm.items.splice(i, 1)"
            ><Trash2 class="size-4"
          /></Button>
        </div>
        <Button
          variant="outline"
          size="sm"
          :disabled="applyForm.items.length >= 100"
          @click="applyForm.items.push({ materialId: '', quantity: '1' })"
          >添加明细</Button
        >
        <p v-if="inventoryLoading" class="mt-3 text-xs text-muted-foreground">
          正在加载可用库存…
        </p>
        <p
          v-else-if="!inventoryRows.length"
          class="mt-3 text-xs text-muted-foreground"
        >
          尚无可用规格，请先新增库存。
        </p>
      </div>
      <div class="sm:col-span-2">
        <label for="material-apply-remark" class="field-label">备注</label
        ><Textarea
          id="material-apply-remark"
          v-model="applyForm.remark"
          :rows="2"
        />
      </div>
      <div
        v-if="isOffice && canApproveAny"
        class="flex items-center justify-between sm:col-span-2"
      >
        <label for="material-direct-issue" class="text-xs"
          >登记后立即发放并扣减库存</label
        ><Switch id="material-direct-issue" v-model="applyForm.directIssue" />
      </div>
    </div>
    <template #footer
      ><Button variant="outline" :disabled="busy" @click="createOpen = false"
        >取消</Button
      ><Button
        :loading="busy"
        :disabled="inventoryLoading"
        @click="createApply"
        >{{ applyForm.directIssue ? "确认发放" : "登记申请" }}</Button
      ></template
    ></Dialog
  >

  <Dialog v-model:open="detailOpen" title="物资单据详情" size="xl"
    ><Callout v-if="detailError" tone="destructive" class="mb-4">{{
      detailError
    }}</Callout>
    <p
      v-if="detailLoading"
      class="py-8 text-center text-sm text-muted-foreground"
    >
      正在加载单据…
    </p>
    <template v-else-if="detail"
      ><div class="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 class="text-base font-semibold">{{ detail.applyNo }}</h2>
          <p class="mt-2 text-xs text-muted-foreground">
            {{ detail.applicantName }} · {{ stateLabel(detail.status) }} ·
            {{ time(detail.createdAt) }}
          </p>
        </div>
        <div v-if="canHandleDetail" class="flex flex-wrap gap-2">
          <Button
            v-if="detail.status === 'pending'"
            size="sm"
            @click="openApplyAction('approve')"
            >通过并扣库存</Button
          ><Button
            v-if="detail.status === 'pending'"
            size="sm"
            variant="destructive"
            @click="openApplyAction('reject')"
            >驳回</Button
          ><Button
            v-if="detail.status === 'borrowed'"
            size="sm"
            @click="openApplyAction('return')"
            >登记归还</Button
          ><Button
            v-if="detail.status === 'borrowed'"
            size="sm"
            variant="outline"
            @click="openApplyAction('extend')"
            >延长借期</Button
          ><Button
            v-if="['returned', 'issued'].includes(detail.status ?? '')"
            size="sm"
            variant="outline"
            @click="openApplyAction('close')"
            >关闭单据</Button
          >
        </div>
      </div>
      <div
        class="mb-5 grid gap-3 rounded-lg bg-muted/30 p-4 text-sm sm:grid-cols-2"
      >
        <p>用途：{{ detail.purpose || "—" }}</p>
        <p>部门：{{ detail.department || "—" }}</p>
        <p v-if="detail.expectReturnDate">
          预计归还：{{ detail.expectReturnDate }}
        </p>
        <p v-if="detail.actualReturnDate">
          实际归还：{{ detail.actualReturnDate }}
        </p>
        <p class="sm:col-span-2">备注：{{ detail.remark || "—" }}</p>
      </div>
      <div class="overflow-x-auto">
        <table class="data-table">
          <thead>
            <tr>
              <th>物资</th>
              <th>规格</th>
              <th>申请数量</th>
              <th>批准数量</th>
              <th>已归还</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in detail.items ?? []" :key="item.id">
              <td>{{ item.materialName }}</td>
              <td>{{ item.spec || item.materialSku || "标准规格" }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ item.approvedQty ?? "—" }}</td>
              <td>{{ item.returnedQuantity ?? 0 }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <h3 class="mb-3 mt-6 text-sm font-semibold">操作记录</h3>
      <div
        v-for="log in detail.logs ?? []"
        :key="log.id"
        class="mb-3 border-l-2 border-primary/30 pl-4"
      >
        <p class="text-xs">
          {{ operationLabel(log.operation) }} ·
          {{ log.operatorName || log.operatorUsername }}
          <span class="text-muted-foreground">{{ time(log.createdAt) }}</span>
        </p>
        <p class="mt-1 text-xs text-muted-foreground">
          {{ stateLabel(log.beforeValue) }} → {{ stateLabel(log.afterValue)
          }}<span v-if="log.remark"> · {{ log.remark }}</span>
        </p>
      </div></template
    ></Dialog
  >

  <Dialog
    v-model:open="applyActionOpen"
    :title="operationLabel(applyAction)"
    size="lg"
    ><Callout v-if="applyActionError" tone="destructive" class="mb-4">{{
      applyActionError
    }}</Callout
    ><template v-if="applyAction === 'approve' || applyAction === 'return'"
      ><p class="mb-4 text-xs text-muted-foreground">
        {{
          applyAction === "approve"
            ? "可减少批准数量；至少批准一件。通过后按批准数量扣减库存。"
            : "填写本次归还数量，已归还的数量不可再次入库。"
        }}
      </p>
      <div
        v-for="item in actionItems"
        :key="item.id"
        class="mb-3 flex items-center gap-3"
      >
        <div class="min-w-0 flex-1">
          <p class="text-sm font-medium">{{ item.name }}</p>
          <p class="mt-1 text-xs text-muted-foreground">
            {{ item.spec }} ·
            {{ applyAction === "return" ? "待归还" : "申请" }} {{ item.max }} 件
          </p>
        </div>
        <Input
          v-model="item.quantity"
          type="number"
          min="0"
          :max="item.max"
          step="1"
          :aria-label="`${item.name}的${applyAction === 'return' ? '归还' : '批准'}数量`"
          class="w-24"
        /></div
    ></template>
    <div v-if="applyAction === 'extend'">
      <label for="material-extend-date" class="field-label"
        >新的预计归还日期</label
      ><Input
        id="material-extend-date"
        v-model="extensionDate"
        type="date"
        :min="detail?.expectReturnDate"
      />
    </div>
    <label for="material-action-remark" class="field-label mt-4">{{
      applyAction === "reject" ? "驳回原因（必填）" : "备注"
    }}</label
    ><Textarea
      id="material-action-remark"
      v-model="actionRemark"
      :rows="3"
    /><template #footer
      ><Button
        variant="outline"
        :disabled="busy"
        @click="applyActionOpen = false"
        >取消</Button
      ><Button
        :variant="applyAction === 'reject' ? 'destructive' : 'default'"
        :loading="busy"
        @click="performApplyAction"
        >确认{{ operationLabel(applyAction) }}</Button
      ></template
    ></Dialog
  >
  <ConfirmDialog
    v-model:open="recordActionOpen"
    :title="recordActionTitle"
    :description="recordActionDescription"
    :tone="recordAction === 'delete' ? 'destructive' : 'default'"
    :busy="busy"
    @confirm="performRecordAction"
  />
</template>
<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Plus, RefreshCw, Search, Trash2 } from "@lucide/vue";
import PortalRowActions from "../../components/portal/PortalRowActions.vue";
import PageHeader from "../../ui/PageHeader.vue";
import Button from "../../ui/Button.vue";
import Input from "../../ui/Input.vue";
import Textarea from "../../ui/Textarea.vue";
import Select from "../../ui/Select.vue";
import Switch from "../../ui/Switch.vue";
import Badge from "../../ui/Badge.vue";
import Callout from "../../ui/Callout.vue";
import Dialog from "../../ui/Dialog.vue";
import ConfirmDialog from "../../ui/ConfirmDialog.vue";
import Pagination from "../../ui/Pagination.vue";
import EmptyState from "../../ui/EmptyState.vue";
import SegmentedTabs from "../../ui/SegmentedTabs.vue";
import PortalImageField from "../../components/portal/PortalImageField.vue";
import {
  materialApi,
  type MaterialRow,
  type MaterialOptions,
} from "../../lib/portal-materials";
import { session } from "../../lib/session";
import { errText, toast } from "../../lib/toast";
const route = useRoute(),
  router = useRouter();
const kind = computed(() =>
    route.query.kind === "office" ? "office" : "sample",
  ),
  isOffice = computed(() => kind.value === "office");
const tabs = computed(() => [
  { value: "products", label: isOffice.value ? "用品目录" : "样品目录" },
  { value: "inventory", label: isOffice.value ? "库存管理" : "台账盘点" },
  { value: "applies", label: isOffice.value ? "领用记录" : "借用归还" },
  { value: "owners", label: "站点负责人" },
]);
const tab = computed(() =>
  tabs.value.some((v) => v.value === route.query.tab)
    ? String(route.query.tab)
    : "inventory",
);
const sites = [
  { value: "SZ", label: "深圳" },
  { value: "CD", label: "成都" },
];
const canConfigureOwners = computed(() =>
    session.has("can_configure_site_owners"),
  ),
  canDelete = computed(() => session.has("can_delete")),
  canApproveAny = computed(() => session.has("can_approve_any_material_apply"));
const options = ref<MaterialOptions & { brands?: string[] }>({
    users: [],
    owners: [],
    products: [],
    categories: [],
  }),
  optionsLoading = ref(false),
  optionsError = ref(""),
  ownerDraft = reactive<Record<string, string>>({ SZ: "", CD: "" });
const userOptions = computed(() =>
    options.value.users.map((user) => ({
      value: user.username,
      label: `${user.name || user.username}（${user.username}）`,
    })),
  ),
  productOptions = computed(() =>
    options.value.products.map((product) => ({
      value: String(product.id),
      label: product.name || String(product.id),
    })),
  ),
  categoryOptions = computed(() =>
    options.value.categories.map((category) => ({
      value: String(category.id),
      label: category.name,
    })),
  );
const brandOptions = computed(() => [
  { value: '', label: '全部品牌' },
  ...[...new Set([...(options.value.brands ?? []), ...(brandFilter.value ? [brandFilter.value] : [])])].sort().map(brand => ({ value: brand, label: brand })),
]);
interface MaterialSummary { productCount?: number; skuCount?: number; stockQty?: number; borrowedQty?: number; outOfStockCount?: number }
const stockSummary = ref<MaterialSummary | null>(null);
const summaryArchived = ref(false);
const stockMetrics = computed(() => [
  { label: '商品数', value: stockSummary.value?.productCount },
  { label: '规格数', value: stockSummary.value?.skuCount },
  { label: '可用库存', value: stockSummary.value?.stockQty },
  { label: isOffice.value ? '零库存规格' : '在借数量', value: isOffice.value ? stockSummary.value?.outOfStockCount : stockSummary.value?.borrowedQty },
]);
const rows = ref<MaterialRow[]>([]),
  page = ref(1),
  total = ref(0),
  search = ref(""),
  archiveFilter = ref("false"),
  loading = ref(false),
  loadError = ref(""),
  busy = ref(false);
const categoryFilter = ref(''), brandFilter = ref(''), siteFilter = ref(''),
  stockFilter = ref(''), statusFilter = ref('');
const applyStatusOptions = computed(() => [
  { value: '', label: '全部状态' }, { value: 'pending', label: '待审批' },
  ...(isOffice.value ? [{ value: 'issued', label: '已发放' }] : [{ value: 'borrowed', label: '借用中' }, { value: 'returned', label: '已归还' }]),
  { value: 'rejected', label: '已驳回' }, { value: 'closed', label: '已关闭' },
]);
const hasFilters = computed(() => Boolean(search.value || categoryFilter.value || brandFilter.value || siteFilter.value || stockFilter.value || statusFilter.value || archiveFilter.value === 'true'));
function readFilters() {
  const text = (key: string) => typeof route.query[key] === 'string' ? String(route.query[key]) : '';
  search.value = text('q');
  categoryFilter.value = /^[1-9]\d*$/.test(text('categoryId')) ? text('categoryId') : '';
  brandFilter.value = text('brand');
  siteFilter.value = sites.some(site => site.value === text('site')) ? text('site') : '';
  stockFilter.value = ['available', 'out'].includes(text('stockState')) ? text('stockState') : '';
  statusFilter.value = applyStatusOptions.value.some(status => status.value === text('status')) ? text('status') : '';
  archiveFilter.value = text('archived') === 'true' ? 'true' : 'false';
  page.value = /^[1-9]\d{0,6}$/.test(text('page')) ? Number(text('page')) : 1;
}
readFilters();
function applyFilters() {
  const query: Record<string, string> = { kind: kind.value, tab: tab.value };
  if (search.value.trim()) query.q = search.value.trim();
  if (tab.value !== 'applies') {
    if (archiveFilter.value === 'true') query.archived = 'true';
    if (categoryFilter.value) query.categoryId = categoryFilter.value;
    if (brandFilter.value.trim()) query.brand = brandFilter.value.trim();
  }
  if (siteFilter.value) query.site = siteFilter.value;
  if (tab.value === 'inventory' && stockFilter.value) query.stockState = stockFilter.value;
  if (tab.value === 'applies' && statusFilter.value) query.status = statusFilter.value;
  const target = router.resolve({ query });
  if (target.fullPath === route.fullPath) void load();
  else void router.push({ query });
}
function resetFilters() {
  search.value = categoryFilter.value = brandFilter.value = siteFilter.value = stockFilter.value = statusFilter.value = '';
  archiveFilter.value = 'false';
  applyFilters();
}
function changePage(value: number) {
  void router.push({ query: { ...route.query, page: value > 1 ? String(value) : undefined } });
}
let loadRequest = 0,
  optionRequest = 0,
  inventoryRequest = 0,
  detailRequest = 0;
async function loadOptions() {
  const id = ++optionRequest;
  optionsLoading.value = true;
  optionsError.value = "";
  try {
    const result = await materialApi.options(kind.value);
    if (id === optionRequest) {
      options.value = result;
      for (const site of sites)
        ownerDraft[site.value] = ownerOf(site.value)?.ownerUsername ?? "";
    }
  } catch (e) {
    if (id === optionRequest) optionsError.value = errText(e);
  } finally {
    if (id === optionRequest) optionsLoading.value = false;
  }
}
async function load() {
  const id = ++loadRequest;
  const requestedArchived = archiveFilter.value === 'true';
  if (tab.value === "owners") { loading.value = false; return; }
  loading.value = true;
  loadError.value = "";
  try {
    const result = await materialApi.page({
      kind: kind.value,
      resource: tab.value,
      q: search.value || undefined,
      archived: archiveFilter.value,
      categoryId: tab.value !== 'applies' ? categoryFilter.value || undefined : undefined,
      brand: tab.value !== 'applies' ? brandFilter.value.trim() || undefined : undefined,
      site: siteFilter.value || undefined,
      stockState: tab.value === 'inventory' ? stockFilter.value || undefined : undefined,
      status: tab.value === 'applies' ? statusFilter.value || undefined : undefined,
      pageNo: page.value,
      pageSize: 20,
    });
    if (id === loadRequest) {
      rows.value = result.list;
      total.value = result.total;
      stockSummary.value = (result as typeof result & { summary?: MaterialSummary }).summary ?? null;
      summaryArchived.value = requestedArchived;
      const lastPage = Math.max(1, Math.ceil(Number(result.total) / 20));
      if (page.value > lastPage) changePage(lastPage);
    }
  } catch (e) {
    if (id === loadRequest) {
      loadError.value = errText(e);
      rows.value = [];
      total.value = 0;
      stockSummary.value = null;
    }
  } finally {
    if (id === loadRequest) loading.value = false;
  }
}
async function reload() {
  await Promise.allSettled([loadOptions(), load()]);
}
function changeTab(value: string) {
  router.push({ query: { kind: kind.value, tab: value } });
}
function siteName(value?: string) {
  return sites.find((site) => site.value === value)?.label ?? value ?? "—";
}
function categoryName(id?: number) {
  return (
    options.value.categories.find((category) => category.id === id)?.name ??
    (id ? `#${id}` : "—")
  );
}
function ownerOf(site: string) {
  return options.value.owners.find((owner) => owner.site === site);
}
function time(value?: string) {
  return value ? value.replace("T", " ").slice(0, 16) : "—";
}
function stateLabel(value?: string) {
  return value
    ? ((
        {
          pending: "待审批",
          borrowed: "借用中",
          returned: "已归还",
          issued: "已发放",
          rejected: "已驳回",
          closed: "已关闭",
        } as Record<string, string>
      )[value] ?? value)
    : "—";
}
function operationLabel(value: string) {
  return (
    (
      {
        create: "登记申请",
        approve: "审批通过",
        reject: "驳回",
        return: "登记归还",
        extend: "延长借期",
        close: "关闭单据",
      } as Record<string, string>
    )[value] ?? value
  );
}
function message(e: unknown) {
  return e instanceof Error ? e.message : errText(e);
}
async function saveOwner(site: string) {
  busy.value = true;
  try {
    await materialApi.owner({
      kind: kind.value,
      site,
      username: ownerDraft[site],
    });
    toast("站点负责人已保存");
    await loadOptions();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
const editOpen = ref(false),
  editResource = ref("products"),
  editId = ref<number>(),
  editError = ref(""),
  editImages = ref<string[]>([]);
const productForm = reactive({
  name: "",
  brand: "",
  categoryId: "",
  description: "",
  variantDims: [] as string[],
});
const skuForm = reactive({
  productId: "",
  site: "SZ",
  variantValues: {} as Record<string, string>,
  sku: "",
  stockQty: "0",
  unitPrice: "",
  location: "",
  note: "",
});
const selectedProduct = computed(() =>
  options.value.products.find(
    (product) => String(product.id) === String(skuForm.productId),
  ),
);
function resetVariantValues() {
  skuForm.variantValues = Object.fromEntries(
    (selectedProduct.value?.variantDims ?? []).map((dim) => [dim, ""]),
  );
}
function openEdit(row?: MaterialRow) {
  editResource.value = tab.value;
  editId.value = row?.id;
  editError.value = "";
  editImages.value = [...(row?.imageUrls ?? [])];
  Object.assign(productForm, {
    name: row?.name ?? "",
    brand: row?.brand ?? "",
    categoryId: row?.categoryId ? String(row.categoryId) : "",
    description: row?.description ?? "",
    variantDims: [...(row?.variantDims ?? [])],
  });
  Object.assign(skuForm, {
    productId: row?.productId ? String(row.productId) : "",
    site: row?.site ?? "SZ",
    variantValues: { ...(row?.variantValues ?? {}) },
    sku: row?.sku ?? "",
    stockQty: String(row?.stockQty ?? 0),
    unitPrice: row?.unitPrice == null ? "" : String(row.unitPrice),
    location: row?.location ?? (isOffice.value ? "办公室" : "陈列室"),
    note: row?.note ?? "",
  });
  editOpen.value = true;
}
async function saveRecord() {
  if (busy.value) return;
  busy.value = true;
  editError.value = "";
  try {
    if (editResource.value === "products") {
      if (!productForm.name.trim() || !productForm.categoryId)
        throw new Error("请填写商品名并选择分类");
      if (productForm.variantDims.some((dim) => !dim.trim()))
        throw new Error("规格维度名称不能为空");
      if (
        new Set(productForm.variantDims.map((v) => v.trim())).size !==
        productForm.variantDims.length
      )
        throw new Error("规格维度名称不能重复");
    } else {
      if (!skuForm.productId) throw new Error("请选择所属商品");
      if (
        !Number.isInteger(Number(skuForm.stockQty)) ||
        Number(skuForm.stockQty) < 0
      )
        throw new Error("库存数量须为非负整数");
    }
    const body =
      editResource.value === "products"
        ? {
            ...productForm,
            categoryId: Number(productForm.categoryId),
            name: productForm.name.trim(),
            variantDims: productForm.variantDims.map((v) => v.trim()),
          }
        : {
            ...skuForm,
            productId: Number(skuForm.productId),
            stockQty: Number(skuForm.stockQty),
            unitPrice:
              skuForm.unitPrice === "" ? null : Number(skuForm.unitPrice),
          };
    await materialApi.save(editResource.value, {
      ...body,
      id: editId.value,
      kind: kind.value,
      imageUrls: editImages.value.filter(Boolean),
    });
    editOpen.value = false;
    toast("台账已保存");
    await reload();
  } catch (e) {
    editError.value = message(e);
  } finally {
    busy.value = false;
  }
}
const recordActionOpen = ref(false),
  recordAction = ref("archive"),
  recordActionRow = ref<MaterialRow>(),
  recordResource = ref("");
const recordActionTitle = computed(() =>
  recordAction.value === "delete"
    ? "永久删除记录？"
    : recordAction.value === "restore"
      ? "恢复记录？"
      : "归档记录？",
);
const recordActionDescription = computed(() =>
  recordAction.value === "delete"
    ? `《${recordActionRow.value?.name ?? ""}》删除后无法恢复。存在规格或历史单据引用时不能删除。`
    : recordAction.value === "restore"
      ? "恢复后该记录重新进入正常台账。"
      : recordResource.value === "products"
        ? "归档商品会同时归档其未归档规格；已有借用与历史记录保留。"
        : "归档规格后不能再用于新申请；已有借用与历史记录保留。",
);
function askRecordAction(row: MaterialRow, action: string) {
  recordActionRow.value = row;
  recordAction.value = action;
  recordResource.value = tab.value;
  recordActionOpen.value = true;
}
async function performRecordAction() {
  if (!recordActionRow.value || busy.value) return;
  busy.value = true;
  try {
    if (recordAction.value === "delete")
      await materialApi.remove(recordResource.value, recordActionRow.value.id);
    else
      await materialApi.archive(
        recordResource.value,
        recordActionRow.value.id,
        recordAction.value === "restore",
      );
    recordActionOpen.value = false;
    toast("操作已完成");
    await reload();
  } catch (e) {
    toast(errText(e), "bad");
  } finally {
    busy.value = false;
  }
}
const createOpen = ref(false),
  createError = ref(""),
  inventoryRows = ref<MaterialRow[]>([]),
  inventoryLoading = ref(false);
const today = new Date().toLocaleDateString("en-CA", {
  timeZone: "Asia/Shanghai",
});
const applyForm = reactive({
  applicantUsername: "",
  expectReturnDate: "",
  purpose: "",
  remark: "",
  directIssue: false,
  items: [{ materialId: "", quantity: "1" }],
});
const inventoryOptions = computed(() =>
  inventoryRows.value.map((row) => ({
    value: String(row.id),
    label: `${row.name} · ${row.spec || "标准规格"} · ${siteName(row.site)} · 库存 ${row.stockQty}`,
  })),
);
async function loadInventory() {
  const request = ++inventoryRequest, requestedKind = kind.value;
  inventoryLoading.value = true;
  try {
    const all: MaterialRow[] = [];
    let no = 1,
      count = 0;
    do {
      const result = await materialApi.page({
        kind: requestedKind,
        resource: "inventory",
        pageNo: no++,
        pageSize: 200,
      });
      if (request !== inventoryRequest || requestedKind !== kind.value) return;
      count = result.total;
      all.push(...result.list);
      if (!result.list.length) break;
    } while (all.length < count);
    if (request === inventoryRequest) inventoryRows.value = all;
  } catch (e) {
    if (request === inventoryRequest) {
      createError.value = errText(e);
      inventoryRows.value = [];
    }
  } finally {
    if (request === inventoryRequest) inventoryLoading.value = false;
  }
}
function openCreateApply() {
  Object.assign(applyForm, {
    applicantUsername: options.value.currentUsername || "",
    expectReturnDate: "",
    purpose: "",
    remark: "",
    directIssue: false,
    items: [{ materialId: "", quantity: "1" }],
  });
  createError.value = "";
  createOpen.value = true;
  loadInventory();
}
async function createApply() {
  if (busy.value) return;
  busy.value = true;
  createError.value = "";
  try {
    if (!applyForm.applicantUsername) throw new Error("请选择申请人");
    if (
      !applyForm.items.length ||
      applyForm.items.some(
        (item) =>
          !item.materialId ||
          !Number.isInteger(Number(item.quantity)) ||
          Number(item.quantity) <= 0,
      )
    )
      throw new Error("请选择至少一项规格，并填写正整数数量");
    if (
      new Set(applyForm.items.map((item) => item.materialId)).size !==
      applyForm.items.length
    )
      throw new Error("同一规格不能重复选择");
    if (!isOffice.value && !applyForm.expectReturnDate)
      throw new Error("请填写预计归还日期");
    await materialApi.createApply({
      ...applyForm,
      kind: kind.value,
      expectReturnDate: isOffice.value ? "" : applyForm.expectReturnDate,
      items: applyForm.items.map((item) => ({
        materialId: Number(item.materialId),
        quantity: Number(item.quantity),
      })),
    });
    createOpen.value = false;
    toast("单据已登记");
    await reload();
  } catch (e) {
    createError.value = message(e);
  } finally {
    busy.value = false;
  }
}
const detailOpen = ref(false),
  detailLoading = ref(false),
  detailError = ref(""),
  detail = ref<MaterialRow>();
const canHandleDetail = computed(
  () =>
    detail.value?.canHandle ??
    (canApproveAny.value ||
      options.value.owners.some(
        (owner) => owner.ownerUsername === options.value.currentUsername,
      )),
);
async function openDetail(id: number) {
  const request = ++detailRequest;
  detailOpen.value = true;
  detail.value = undefined;
  detailLoading.value = true;
  detailError.value = "";
  try {
    const result = await materialApi.detail(id);
    if (request === detailRequest) detail.value = result;
  } catch (e) {
    if (request === detailRequest) detailError.value = errText(e);
  } finally {
    if (request === detailRequest) detailLoading.value = false;
  }
}
const applyActionOpen = ref(false),
  applyAction = ref("approve"),
  applyActionError = ref(""),
  actionRemark = ref(""),
  extensionDate = ref(""),
  actionItems = ref<
    { id: number; name: string; spec: string; max: number; quantity: string }[]
  >([]);
function openApplyAction(action: string) {
  if (!detail.value) return;
  applyAction.value = action;
  applyActionError.value = "";
  actionRemark.value = "";
  extensionDate.value = "";
  actionItems.value = (detail.value.items ?? [])
    .map((item: any) => {
      const max =
        action === "return"
          ? Number(item.approvedQty ?? item.quantity) -
            Number(item.returnedQuantity ?? 0)
          : Number(item.quantity);
      return {
        id: item.id,
        name: item.materialName,
        spec: item.spec || "",
        max,
        quantity: String(max),
      };
    })
    .filter((item: any) => item.max > 0);
  applyActionOpen.value = true;
}
async function performApplyAction() {
  if (!detail.value || busy.value) return;
  busy.value = true;
  applyActionError.value = "";
  try {
    if (applyAction.value === "reject" && !actionRemark.value.trim())
      throw new Error("请填写驳回原因");
    if (applyAction.value === "extend" && !extensionDate.value)
      throw new Error("请填写新的归还日期");
    if (
      ["approve", "return"].includes(applyAction.value) &&
      actionItems.value.some(
        (item) =>
          !Number.isInteger(Number(item.quantity)) ||
          Number(item.quantity) < 0 ||
          Number(item.quantity) > item.max,
      )
    )
      throw new Error("数量须为有效范围内的整数");
    const items = actionItems.value
      .map((item) => ({ id: item.id, quantity: Number(item.quantity) }))
      .filter((item) => applyAction.value !== "return" || item.quantity > 0);
    if (applyAction.value === 'return' && !items.length)
      throw new Error('请填写至少一项正整数归还数量');
    await materialApi.action({
      id: detail.value.id,
      action: applyAction.value,
      remark: actionRemark.value.trim(),
      expectReturnDate: extensionDate.value,
      items,
    });
    applyActionOpen.value = false;
    toast("单据已更新");
    const id = detail.value.id;
    await Promise.allSettled([openDetail(id), load()]);
  } catch (e) {
    applyActionError.value = message(e);
  } finally {
    busy.value = false;
  }
}
let contextKind = kind.value, contextTab = tab.value;
watch(() => route.fullPath, () => {
  const changedKind = kind.value !== contextKind;
  if (changedKind || tab.value !== contextTab) {
    editOpen.value = createOpen.value = detailOpen.value = applyActionOpen.value = false;
    inventoryRequest++;
    detailRequest++;
    inventoryLoading.value = detailLoading.value = false;
    inventoryRows.value = [];
    stockSummary.value = null;
  }
  if (changedKind) {
    options.value = { users: [], owners: [], products: [], categories: [] };
    void loadOptions();
  }
  contextKind = kind.value;
  contextTab = tab.value;
  readFilters();
  void load();
});
onMounted(reload);
</script>
