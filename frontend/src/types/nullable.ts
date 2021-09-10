export type Nullable<T> = T | null;

export type Undefinable<T> = T | undefined;

export type Optional<T> = Undefinable<Nullable<T>>;
