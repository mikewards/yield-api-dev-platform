/**
 * Flow Applications SDK
 * Embeddable UI component for managing Flow applications
 */

export interface FlowApplicationsConfig {
  apiKey: string;
  container: string | HTMLElement;
  theme?: 'light' | 'dark';
  primaryColor?: string;
  onApplicationCreated?: (app: any) => void;
  onApplicationUpdated?: (app: any) => void;
  hideHeader?: boolean;
}

export class FlowApplications {
  private config: FlowApplicationsConfig;
  private container: HTMLElement;
  private iframe: HTMLIFrameElement | null = null;

  constructor(config: FlowApplicationsConfig) {
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
      theme: this.config.theme || 'light',
      ...(this.config.primaryColor && { primaryColor: this.config.primaryColor }),
      ...(this.config.hideHeader && { hideHeader: 'true' })
    });

    this.iframe = document.createElement('iframe');
    this.iframe.src = `https://sdks.flow.com/applications?${params.toString()}`;
    this.iframe.width = '100%';
    this.iframe.height = '600';
    this.iframe.frameBorder = '0';
    this.iframe.allowTransparency = true;
    this.iframe.style.border = 'none';

    // Listen for messages from iframe
    window.addEventListener('message', (event) => {
      if (event.origin !== 'https://sdks.flow.com') return;

      if (event.data.type === 'application.created' && this.config.onApplicationCreated) {
        this.config.onApplicationCreated(event.data.application);
      }
      if (event.data.type === 'application.updated' && this.config.onApplicationUpdated) {
        this.config.onApplicationUpdated(event.data.application);
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

