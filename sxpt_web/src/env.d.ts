/// <reference types="vite/client" />

declare module '@open-file-viewer/core' {
  export interface FileViewer {
    resize(): void;
    destroy(): void;
  }

  export interface PreviewPlugin {
    name?: string;
  }

  export interface ViewerOptions {
    container: HTMLElement;
    file: string;
    fileName?: string;
    mimeType?: string;
    width?: string | number;
    height?: string | number;
    fit?: 'contain' | 'cover' | 'width' | 'height' | string;
    locale?: string;
    theme?: 'light' | 'dark' | string;
    fallback?: 'inline' | 'download' | string;
    toolbar?: Record<string, boolean>;
    plugins?: PreviewPlugin[];
    onLoad?: () => void;
    onError?: (error: Error) => void;
    onUnsupported?: () => void;
  }

  export interface PdfPluginOptions {
    workerSrc?: string;
    useFetchData?: boolean;
  }

  export function createViewer(options: ViewerOptions): FileViewer;
  export function imagePlugin(): PreviewPlugin;
  export function videoPlugin(): PreviewPlugin;
  export function audioPlugin(): PreviewPlugin;
  export function officePlugin(options?: { pdf?: PdfPluginOptions }): PreviewPlugin;
  export function pdfPlugin(options?: PdfPluginOptions): PreviewPlugin;
  export function textPlugin(): PreviewPlugin;
  export function fallbackPlugin(): PreviewPlugin;
}

declare module '@open-file-viewer/core/style.css';
