import { gsap } from 'gsap';
import Lenis from 'lenis';
let dispose = () => {};
function onPage() {
  dispose();
  const topics = document.querySelector<HTMLButtonElement>('.docs-topics-toggle');
  const toggleTopics = () => {
    const open = topics?.getAttribute('aria-expanded') !== 'true';
    topics?.setAttribute('aria-expanded', String(open));
    document.querySelector('.docs-sidebar')?.toggleAttribute('data-open', open);
  };
  topics?.addEventListener('click', toggleTopics);
  const reduce = matchMedia('(prefers-reduced-motion: reduce)').matches;
  let lenis: Lenis | undefined;
  let tick: ((time: number) => void) | undefined;
  if (!reduce && document.body.dataset.page === 'home' && matchMedia('(hover: hover)').matches) {
    lenis = new Lenis({ anchors: true }); tick = time => lenis?.raf(time * 1000); gsap.ticker.add(tick);
  }
  const slider = document.querySelector<HTMLInputElement>('#trip-count');
  const value = document.querySelector('#trip-value'); const total = document.querySelector('#example-total');
  const result = document.querySelector('#example-result'); const progress = document.querySelector('.track-progress');
  const traveller = document.querySelector('.traveller');
  const update = () => {
    if (!slider) return;
    const count = Number(slider.value);
    if (value) value.textContent = String(count);
    if (total) total.textContent = `S$${(count * 2).toFixed(2)}`;
    if (result) result.textContent = count < 24 ? `${24-count} more trips would cover the pass.` : count === 24 ? 'The pass cost is covered.' : `S$${(count*2-48).toFixed(2)} above the pass price.`;
    gsap.to(progress, { clipPath: `inset(0 ${100 - count / 36 * 100}% 0 0)`, duration: reduce ? 0 : .35, ease: 'power3.out', overwrite: true });
    const track = document.querySelector<HTMLElement>('.journey-track');
    if (track && traveller) gsap.to(traveller, { x: (count / 36 - .5) * track.clientWidth, duration: reduce ? 0 : .35, ease: 'power3.out', overwrite: true });
  };
  slider?.addEventListener('input', update);
  const resize = new ResizeObserver(update); const track = document.querySelector('.journey-track');
  if (track) resize.observe(track); update();
  dispose = () => { topics?.removeEventListener('click', toggleTopics); slider?.removeEventListener('input', update); resize.disconnect(); if (tick) gsap.ticker.remove(tick); lenis?.destroy(); gsap.killTweensOf([progress, traveller].filter(Boolean)); };
}
document.addEventListener('astro:before-preparation', event => {
  if (matchMedia('(prefers-reduced-motion: reduce)').matches) return;
  const load = event.loader;
  event.loader = async () => {
    await Promise.all([load(), gsap.to('#main', { x: -12, opacity: 0, duration: .14, ease: 'power3.out' })]);
  };
});
document.addEventListener('astro:after-swap', () => {
  document.documentElement.classList.add('js');
  if (!matchMedia('(prefers-reduced-motion: reduce)').matches) {
    gsap.fromTo('#main', { x: 12, opacity: 0 }, { x: 0, opacity: 1, duration: .2, ease: 'power3.out', clearProps: 'transform,opacity' });
  }
});
document.addEventListener('astro:before-swap', () => dispose());
document.addEventListener('astro:page-load', onPage);
