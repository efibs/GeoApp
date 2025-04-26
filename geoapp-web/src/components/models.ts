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
