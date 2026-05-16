/**
 * Flow Yield Accounts SDK
 * Embeddable UI component for managing yield accounts
 */

export interface FlowYieldAccountsConfig {
  apiKey: string;
  walletId: string;
  container: string | HTMLElement;
  theme?: 'light' | 'dark';
  primaryColor?: string;
  onAccountCreated?: (account: any) => void;
  onDeposit?: (transaction: any) => void;
  onWithdraw?: (transaction: any) => void;
}

export class FlowYieldAccounts {
  private config: FlowYieldAccountsConfig;
  private container: HTMLElement;
  private iframe: HTMLIFrameElement | null = null;

  constructor(config: FlowYieldAccountsConfig) {
    this.config = config;
    this.container = typeof config.container === 'string'
      ? document.querySelector(config.container) as HTMLElement
      : config.container;

    if (!this.container) {
      throw new Error('Container element not found');
    }
  }

  render(): void {
    const params = new URLSearchParams({
      apiKey: this.config.apiKey,
      walletId: this.config.walletId,
      theme: this.config.theme || 'light',
      ...(this.config.primaryColor && { primaryColor: this.config.primaryColor })
    });

    this.iframe = document.createElement('iframe');
    this.iframe.src = `https://sdks.flow.com/yield-accounts?${params.toString()}`;
    this.iframe.width = '100%';
    this.iframe.height = '700';
    this.iframe.frameBorder = '0';
    this.iframe.style.border = 'none';

    // Listen for messages from iframe
    window.addEventListener('message', (event) => {
      if (event.origin !== 'https://sdks.flow.com') return;

      if (event.data.type === 'account.created' && this.config.onAccountCreated) {
        this.config.onAccountCreated(event.data.account);
      }
      if (event.data.type === 'deposit.completed' && this.config.onDeposit) {
        this.config.onDeposit(event.data.transaction);
      }
      if (event.data.type === 'withdraw.completed' && this.config.onWithdraw) {
        this.config.onWithdraw(event.data.transaction);
      }
    });

    this.container.appendChild(this.iframe);
  }

  destroy(): void {
    if (this.iframe) {
      this.iframe.remove();
      this.iframe = null;
    }
  }
}

