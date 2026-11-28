export type WeeklyReward = {
  weekStart: string;
  weekEnd: string;
  grantedAt?: string;
  top: {
    rank: number;
    sharedRecipeId: number;
    title: string;
    saveCount: number;
    authorId: number;
    reward: number;
    granted: boolean;
  }[];
};

export type RewardHistory = {
  weekStart: string;
  winners: { rank: number; userId: number }[];
  totalReward: number;
};

export type WeeklyRewardsData = {
  weeks: string[];
  weeksData: WeeklyReward[];
  history: RewardHistory[];
};
