import { useCallback, useEffect, useState } from 'react';
import { ExpenseSheet, type SheetTarget } from './components/ExpenseSheet';
import { IconSprite } from './components/Icon';
import { Toast } from './components/toast';
import { useRoute } from './hooks/useRoute';
import { useToday } from './hooks/useToday';
import { Dashboard } from './screens/Dashboard';
import { History } from './screens/History';
import { Onboarding } from './screens/Onboarding';
import { Paywall } from './screens/Paywall';
import { Settings } from './screens/Settings';
import { useBudgetStore } from './store/useBudgetStore';

export function App() {
  const today = useToday();
  const route = useRoute();
  const onboarded = useBudgetStore((s) => s.settings.onboarded);
  const theme = useBudgetStore((s) => s.settings.theme);
  const [sheet, setSheet] = useState<SheetTarget>(null);
  const closeSheet = useCallback(() => setSheet(null), []);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') delete root.dataset.app;
    else root.dataset.app = theme;
  }, [theme]);

  let screen;
  if (!onboarded) screen = <Onboarding today={today} />;
  else if (route === 'history') screen = <History key={today} today={today} openSheet={setSheet} />;
  else if (route === 'settings') screen = <Settings today={today} />;
  else if (route === 'paywall') screen = <Paywall />;
  else screen = <Dashboard today={today} openSheet={setSheet} />;

  return (
    <div className="app">
      <IconSprite />
      <div key={onboarded ? route : 'onboarding'} className="enter">
        {screen}
      </div>
      <ExpenseSheet target={sheet} today={today} onClose={closeSheet} />
      <Toast />
    </div>
  );
}
