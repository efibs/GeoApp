export interface Todo {
  id: number;
  content: string;
}

export interface Meta {
  totalCount: number;
}

export interface Login {
  username: string;
  password: string;
}

export interface Datapoint {
  latitude: number;
  longitude: number;
  timestamp: string;
}

export interface GenerateToken {
  expiry: string;
  permissions: string[];
}

export const ClaimTypeUserId: string =
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier';

export const PermissionReadData: string = 'perm:ReadData';

export const PermissionWriteData: string = 'perm:WriteData';
