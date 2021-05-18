import 'assets/css/app.scss';

export const parameters = {
    actions: {argTypesRegex: "^on[A-Z].*"},
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
