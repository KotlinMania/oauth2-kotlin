# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 12/17 (70.6%)
- **Function parity:** 173/298 matched (target 387) — 58.1%
- **Class/type parity:** 49/89 matched (target 109) — 55.1%
- **Combined symbol parity:** 222/387 matched (target 496) — 57.4%
- **Average inline-code cosine:** 0.50 (function body across 11 matched files)
- **Average documentation cosine:** 0.61 (doc text across 11 matched files)
- **Cheat-zeroed Files:** 2
- **Critical Issues:** 8 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. error

- **Target:** `oauth2.Error`
- **Similarity:** 0.47
- **Dependents:** 8
- **Priority Score:** 8011005.5
- **Functions:** 5/6 matched (target 17)
- **Missing functions:** `fmt`
- **Types:** 4/4 matched (target 9)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 2. helpers

- **Target:** `oauth2.Helpers [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 546210.0
- **Functions:** 8/46 matched (target 9)
- **Missing functions:** `expecting`, `visit_str`, `visit_none`, `visit_unit`, `visit_seq`, `description`, `fmt`, `custom`, `serialize_bool`, `serialize_i8`, `serialize_i16`, `serialize_i32`, `serialize_i64`, `serialize_u8`, `serialize_u16`, `serialize_u32`, `serialize_u64`, `serialize_f32`, `serialize_f64`, `serialize_char`, `serialize_str`, `serialize_bytes`, `serialize_none`, `serialize_some`, `serialize_unit`, `serialize_unit_struct`, `serialize_unit_variant`, `serialize_newtype_struct`, `serialize_newtype_variant`, `serialize_seq`, `serialize_tuple`, `serialize_tuple_struct`, `serialize_tuple_variant`, `serialize_map`, `serialize_struct`, `serialize_struct_variant`, `serialize_field`, `end`
- **Types:** 0/16 matched (target 1)
- **Missing types:** `StringOrVec`, `Value`, `NotEnum`, `Result`, `VariantName`, `Ok`, `Error`, `SerializeSeq`, `SerializeTuple`, `SerializeTupleStruct`, `SerializeTupleVariant`, `SerializeMap`, `SerializeStruct`, `SerializeStructVariant`, `Enum`, `ObjectWithOptionalStringOrVecString`
- **Tests:** 3/3 matched

### 3. devicecode

- **Target:** `oauth2.Devicecode`
- **Similarity:** 0.36
- **Dependents:** 0
- **Priority Score:** 255506.4
- **Functions:** 21/43 matched (target 40)
- **Missing functions:** `exchange_device_code_impl`, `exchange_device_access_token_impl`, `set_time_fn`, `process_response`, `compute_timeout`, `default_devicecode_interval`, `deserialize_devicecode_interval`, `expecting`, `visit_u64`, `visit_unit`, `from_str`, `as_ref`, `deserialize`, `serialize`, `fmt`, `new_device_auth_details`, `new`, `next`, `mock_time_fn`, `mock_sleep_fn`, `test_device_token_authorization_timeout`, `test_device_auth_response_null_interval`
- **Types:** 9/12 matched (target 17)
- **Missing types:** `NumOrNull`, `Value`, `IncreasingTime`
- **Tests:** 7/14 matched

### 4. revocation

- **Target:** `oauth2.Revocation`
- **Similarity:** 0.36
- **Dependents:** 0
- **Priority Score:** 72506.4
- **Functions:** 14/21 matched (target 22)
- **Missing functions:** `revoke_token_impl`, `from_str`, `as_ref`, `deserialize`, `serialize`, `fmt`, `test_extension_token_revocation_successful`
- **Types:** 4/4 matched (target 9)
- **Missing types:** _none_
- **Tests:** 7/8 matched

### 5. basic

- **Target:** `oauth2.Basic`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 51310.0
- **Functions:** 0/5 matched
- **Missing functions:** `from_str`, `as_ref`, `deserialize`, `serialize`, `fmt`
- **Types:** 8/8 matched (target 18)
- **Missing types:** _none_

### 6. endpoint

- **Target:** `oauth2.Endpoint`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 51305.0
- **Functions:** 6/7 matched (target 17)
- **Missing functions:** `call`
- **Types:** 2/6 matched (target 3)
- **Missing types:** `AsyncHttpClient`, `Error`, `Future`, `SyncHttpClient`
- **Tests:** 1/1 matched

### 7. token.mod

- **Target:** `oauth2.Token [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 43410.0
- **Functions:** 21/25 matched (target 53)
- **Missing functions:** `exchange_client_credentials_impl`, `exchange_code_impl`, `exchange_password_impl`, `exchange_refresh_token_impl`
- **Types:** 9/9 matched (target 10)
- **Missing types:** _none_

### 8. introspection

- **Target:** `oauth2.Introspection`
- **Similarity:** 0.77
- **Dependents:** 0
- **Priority Score:** 34002.3
- **Functions:** 34/36 matched (target 37)
- **Missing functions:** `introspect_impl`, `none_field`
- **Types:** 3/4 matched
- **Missing types:** `TokenType`
- **Tests:** 2/2 matched

### 9. client

- **Target:** `oauth2.Client`
- **Similarity:** 0.42
- **Dependents:** 0
- **Priority Score:** 23705.8
- **Functions:** 30/31 matched (target 37)
- **Missing functions:** `new`
- **Types:** 5/6 matched (target 5)
- **Missing types:** `EndpointStateSealed`

### 10. code

- **Target:** `oauth2.Code`
- **Similarity:** 0.79
- **Dependents:** 0
- **Priority Score:** 12002.1
- **Functions:** 18/19 matched
- **Missing functions:** `authorize_url_impl`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 10/10 matched

### 11. types

- **Target:** `oauth2.Types`
- **Similarity:** 0.79
- **Dependents:** 0
- **Priority Score:** 1702.1
- **Functions:** 16/16 matched (target 131)
- **Missing functions:** _none_
- **Types:** 1/1 matched (target 24)
- **Missing types:** _none_
- **Tests:** 7/7 matched

### 12. lib

- **Target:** `oauth2.Lib`
- **Similarity:** 1.00
- **Dependents:** 0
- **Priority Score:** 300.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 3/3 matched (target 7)
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

