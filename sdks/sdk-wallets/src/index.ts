/**
 * Flow Wallets SDK
 * Embeddable UI component for managing Ethereum wallets
 */

export interface FlowWalletsConfig {
  apiKey: string;
  applicationId: string;
  container: string | HTMLElement;
  theme?: 'light' | 'dark';
  primaryColor?: string;
  onWalletCreated?: (wallet: any) => void;
  onBalanceUpdated?: (wallet: any) => void;
}

export class FlowWallets {
  private config: FlowWalletsConfig;
  private container: HTMLElement;
  private iframe: HTMLIFrameElement | null = null;

  constructor(config: FlowWalletsConfig) {
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
      applicationId: this.config.applicationId,
      theme: this.config.theme || 'light',
      ...(this.config.primaryColor && { primaryColor: this.config.primaryColor })
    });

    this.iframe = document.createElement('iframe');
    this.iframe.src = `https://sdks.flow.com/wallets?${params.toString()}`;
    this.iframe.width = '100%';
    this.iframe.height = '600';
    this.iframe.frameBorder = '0';
    this.iframe.style.border = 'none';

    // Listen for messages from iframe
    window.addEventListener('message', (event) => {
      if (event.origin !== 'https://sdks.flow.com') return;

      if (event.data.type === 'wallet.created' && this.config.onWalletCreated) {
        this.config.onWalletCreated(event.data.wallet);
      }
      if (event.data.type === 'balance.updated' && this.config.onBalanceUpdated) {
        this.config.onBalanceUpdated(event.data.wallet);
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

