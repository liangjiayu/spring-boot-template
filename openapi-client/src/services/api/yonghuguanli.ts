// @ts-ignore
/* eslint-disable */
import request from "umi-request";

/** 查询用户列表 按 ID 升序返回 GET /api/users */
export async function list(options?: { [key: string]: any }) {
  return request<API.UserResponse[]>("/api/users", {
    method: "GET",
    ...(options || {}),
  });
}

/** 新增用户 POST /api/users */
export async function create(
  body: API.UserRequest,
  options?: { [key: string]: any }
) {
  return request<API.UserResponse>("/api/users", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    data: body,
    ...(options || {}),
  });
}

/** 查询用户详情 GET /api/users/${param0} */
export async function get(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.UserResponse>(`/api/users/${param0}`, {
    method: "GET",
    params: { ...queryParams },
    ...(options || {}),
  });
}

/** 修改用户 省略邮箱或年龄，或将其设为 null，会清空对应字段 PUT /api/users/${param0} */
export async function update(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.updateParams,
  body: API.UserRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<API.UserResponse>(`/api/users/${param0}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  });
}

/** 删除用户 DELETE /api/users/${param0} */
export async function deleteUsingDelete(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteUsingDELETEParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params;
  return request<boolean>(`/api/users/${param0}`, {
    method: "DELETE",
    params: { ...queryParams },
    ...(options || {}),
  });
}
