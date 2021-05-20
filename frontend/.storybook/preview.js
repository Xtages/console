import 'assets/css/app.scss';

const customViewports = {
    responsive: {
        name: 'Responsive',
        styles: {
            width: '100%',
            height: '100%',
        },
        type: 'desktop',
    },
    desktop: {
        name: 'Desktop',
        styles: {
            width: '1440px',
            height: '720px',
        },
        type: 'desktop',
    },
    tablet: {
        name: 'Tablet',
        styles: {
            width: '768px',
            height: '720px',
        },
        type: 'tablet',
    },
    defaultViewport: 'desktop',
};

export const parameters = {
    actions: {argTypesRegex: "^on[A-Z].*"},
    viewport: {
        viewports: {
            ...customViewports,
        },
    },
    controls: {
        matchers: {
            color: /(color)$/i,
            date: /Date$/,
        },
    },
    viewMode: 'docs',
    previewTabs: {
        'storybook/docs/panel': { index: -1 },
    },
}
