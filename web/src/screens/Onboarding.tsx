import { useState, type CSSProperties } from 'react';
import { BudgetInput, ClosingDayPicker } from '../components/BudgetFields';
import { Icon } from '../components/Icon';
import { activeRange } from '../lib/calculator';
import { CLOSING_END_OF_MONTH, formatMD, getPeriod, type DateKey } from '../lib/date';
import { formatYen } from '../lib/format';
import { useBudgetStore } from '../store/useBudgetStore';

export function Onboarding({ today }: { today: DateKey }) {
  const [step, setStep] = useState(0);
  const [budget, setBudget] = useState(50000);
  const [closingDay, setClosingDay] = useState(CLOSING_END_OF_MONTH);
  const [showAll, setShowAll] = useState(false);
  const period = getPeriod(today, closingDay);
  const range = activeRange(period, budget, today);

  const next = () => {
    setStep((s) => s + 1);
    window.scrollTo(0, 0);
  };

  return (
    <section className="screen ob">
      <div className="steps" aria-label={`ステップ ${step + 1} / 3`}>
        {[0, 1, 2].map((i) => (
          <i key={i} className={i === step ? 'on' : ''} />
        ))}
      </div>

      {step === 0 && (
        <>
          <h2>今日あといくら使えるか、ひと目でわかる。</h2>
          <div className="ob-demo" aria-hidden="true">
            <div className="hero-label">今日使えるお金</div>
            <div className="big">
              <span className="yen">¥</span>2,450
            </div>
            <span className="chip" style={{ '--st': 'var(--great)' } as CSSProperties}>
              超順調！
            </span>
          </div>
          <ul className="points">
            <li>
              <span className="cat-ico">
                <Icon name="shield" />
              </span>
              <div>
                <b>口座連携なし</b>
                <small>記録はこの端末の中だけに保存します</small>
              </div>
            </li>
            <li>
              <span className="cat-ico">
                <Icon name="tap" />
              </span>
              <div>
                <b>金額とカテゴリだけで記録</b>
                <small>レシート撮影も細かい分類もいりません</small>
              </div>
            </li>
            <li>
              <span className="cat-ico">
                <Icon name="roll" />
              </span>
              <div>
                <b>使わなかった分は明日に回る</b>
                <small>節約した日の翌日は、使える額が増えます</small>
              </div>
            </li>
          </ul>
          <div className="ob-actions">
            <button className="primary" onClick={next}>
              はじめる
            </button>
          </div>
        </>
      )}

      {step === 1 && (
        <>
          <h2>1か月に自由に使えるお金は？</h2>
          <p className="lead">家賃や光熱費など、毎月決まって出ていくお金を除いた金額です。</p>
          <BudgetInput value={budget} onChange={setBudget} />
          <div className="preview">
            <div>
              <span>1日あたり</span>
              <b>約 {formatYen(Math.floor(budget / 30))}</b>
            </div>
          </div>
          <div className="ob-actions">
            <button className="primary" onClick={next} disabled={!budget}>
              次へ
            </button>
            <button className="ghost" onClick={() => setStep(0)}>
              戻る
            </button>
          </div>
        </>
      )}

      {step === 2 && (
        <>
          <h2>毎月何日で区切りますか？</h2>
          <p className="lead">選んだ日の翌日から、新しい月度が始まります。給料日の前日を選ぶ人が多いです。</p>
          <ClosingDayPicker value={closingDay} onChange={setClosingDay} showAll={showAll} />
          {!showAll && (
            <button className="link-btn" onClick={() => setShowAll(true)}>
              ほかの日を選ぶ
            </button>
          )}
          <div className="preview">
            <div>
              <span>今の月度</span>
              <b>
                {formatMD(period.start)}〜{formatMD(period.end)}（{period.days}日間）
              </b>
            </div>
            {range.days < period.days && (
              <div>
                <span>今月度の予算（今日から{range.days}日分）</span>
                <b>{formatYen(range.budget)}</b>
              </div>
            )}
            <div>
              <span>1日あたり</span>
              <b>{formatYen(Math.floor(range.budget / range.days))}</b>
            </div>
          </div>
          <div className="ob-actions">
            <button className="primary" onClick={() => useBudgetStore.getState().completeOnboarding(budget, closingDay, today)}>
              この設定ではじめる
            </button>
            <button className="ghost" onClick={() => setStep(1)}>
              戻る
            </button>
            <div className="privacy">
              <Icon name="shield" />
              入力した内容はこの端末の外に送信されません
            </div>
          </div>
        </>
      )}
    </section>
  );
}
