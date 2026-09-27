import { Icon } from '../components/Icon';
import { toast } from '../components/toast';
import { navigate } from '../hooks/useRoute';

const ROWS: [string, boolean, boolean | 'soon'][] = [
  ['日割り計算・支出の記録', true, true],
  ['履歴カレンダー', true, true],
  ['貯金プールモード', false, true],
  ['CSVで書き出し', false, true],
  ['ホーム画面ウィジェット（iOS版）', false, 'soon'],
];

const Mark = ({ v }: { v: boolean | 'soon' }) =>
  v === 'soon' ? <span className="soon">予定</span> : <Icon name={v ? 'check' : 'minus'} />;

export function Paywall() {
  const notReady = () => toast('購入はまだ準備中です');
  return (
    <section className="screen pw has-dock">
      <div className="pw-top">
        <button className="icon-btn" onClick={() => navigate('settings')} aria-label="閉じる">
          <Icon name="x" />
        </button>
      </div>
      <div className="pw-badge" aria-hidden="true">
        <Icon name="spark" size={40} />
      </div>
      <h2>一度の購入で、ずっと快適に。</h2>
      <p className="sub-copy">月額課金はありません。1回買えば、この先ずっと使えます。</p>
      <table className="cmp">
        <thead>
          <tr>
            <th>機能</th>
            <th>無料</th>
            <th className="pro-col">Pro</th>
          </tr>
        </thead>
        <tbody>
          {ROWS.map(([name, free, pro]) => (
            <tr key={name}>
              <td>{name}</td>
              <td className={free ? 'yes' : 'no'}>
                <Mark v={free} />
              </td>
              <td className={pro ? 'pro-col yes' : 'pro-col no'}>
                <Mark v={pro} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="dock">
        <button className="primary" onClick={notReady}>
          ¥1,000 で永久解放（買い切り）
        </button>
        <div className="pw-foot">
          <button onClick={notReady}>購入を復元</button>
          <button onClick={() => toast('利用規約は準備中です')}>利用規約</button>
          <button onClick={() => toast('プライバシーポリシーは準備中です')}>プライバシー</button>
        </div>
      </div>
    </section>
  );
}
