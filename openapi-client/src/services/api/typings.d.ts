declare namespace API {
  type ApiErrorSchema = {
    /** HTTP 状态码 */
    status: number;
    /** 错误说明 */
    message: string;
  };

  type deleteUsingDELETEParams = {
    /** 用户 ID */
    id: number;
  };

  type getParams = {
    /** 用户 ID */
    id: number;
  };

  type updateParams = {
    /** 用户 ID */
    id: number;
  };

  type UserRequest = {
    /** 用户名，唯一 */
    username: string;
    /** 邮箱，传 null 或省略表示无邮箱 */
    email?: string;
    /** 年龄 */
    age?: number;
  };

  type UserResponse = {
    /** 用户 ID */
    id: number;
    /** 用户名 */
    username: string;
    /** 邮箱 */
    email?: string;
    /** 年龄 */
    age?: number;
    /** 创建时间 */
    createdAt: string;
    /** 更新时间 */
    updatedAt: string;
  };
}
