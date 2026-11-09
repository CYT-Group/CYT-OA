export type SelectValueType = string | number | null | undefined

export interface SelectOption {
  label: string
  value: SelectValueType
  disabled?: boolean
}

export type RawSelectOption = SelectOption | readonly [SelectValueType, string]
