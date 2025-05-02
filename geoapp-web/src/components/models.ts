export interface Todo {
  id: number;
  content: string;
}

export interface Meta {
  totalCount: number;
}

export interface JwtToken {
  token: string;
}

export interface Login {
  username: string;
  password: string;
}

export interface Register {
  username: string;
  password: string;
}

export interface Datapoint {
  latitude: number;
  longitude: number;
  steps: number;
  timestamp: string;
}

export interface GenerateToken {
  expiry: string;
  permissions: string[];
}

export interface ValidationErorr {
  code: string;
  description: string;
}

export const ClaimTypeUserId: string =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier';
export const ClaimTypeUsername: string =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name';
export const ClaimTypeRoles: string =
  'http://schemas.microsoft.com/ws/2008/06/identity/claims/role';

export const PermissionReadData: string = 'perm:ReadData';
export const PermissionWriteData: string = 'perm:WriteData';

export const RoleAdmin: string = 'Administrators';
