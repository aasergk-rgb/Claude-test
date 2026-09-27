export interface Category {
  id: string;
  name: string;
  color: string;
}

export const CATEGORIES: Category[] = [
  { id: 'food', name: '食費', color: '#E9803A' },
  { id: 'cafe', name: 'カフェ・軽食', color: '#A86B3C' },
  { id: 'daily', name: '日用品', color: '#2F9A97' },
  { id: 'transport', name: '交通費', color: '#4F6BD8' },
  { id: 'fun', name: '娯楽', color: '#C454A8' },
  { id: 'other', name: 'その他', color: '#7B8494' },
];

export function getCategory(id: string): Category {
  return CATEGORIES.find((c) => c.id === id) ?? CATEGORIES[CATEGORIES.length - 1];
}
