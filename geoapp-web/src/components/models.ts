import type { JwtPayload } from 'jwt-decode';

export interface Todo {
  id: number;
  content: string;
}

export interface Meta {
  totalCount: number;
}

export interface JwtTokenResponse {
  token: string;
}

export interface JwtToken extends JwtPayload {
  Permissions?: string[];
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier': string;
  'http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name': string;
  'http://schemas.microsoft.com/ws/2008/06/identity/claims/role'?: string[];
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

export interface MapDatapoint {
  lat: number;
  lng: number;
}

export interface MapDataEntry {
  data: MapDatapoint[];
  userId: string;
}

export interface MapPoint {
  data: MapDatapoint;
  userId: string;
}

export type DataColors = { [userId: string]: string };

export interface GenerateToken {
  expiry: string;
  permissions: string[];
}

export interface ValidationErorr {
  code: string;
  description: string;
}

export interface OtherUserDataAllowance {
  userId: string;
  username: string;
  userReadToken: string;
  color: string;
}

export const PermissionReadData: string = 'perm:ReadData';
export const PermissionWriteData: string = 'perm:WriteData';

export const RoleAdmin: string = 'Administrators';
