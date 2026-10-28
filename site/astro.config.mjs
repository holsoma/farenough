// @ts-check
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

// https://astro.build/config
export default defineConfig({ site: 'https://holsoma.github.io', base: '/fairenough', trailingSlash: 'always', integrations: [sitemap()], markdown: { shikiConfig: { theme: 'github-light' } } });
