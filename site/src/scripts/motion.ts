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
  const fare = document.querySelector<HTMLSelectElement>('#example-fare');
  const value = document.querySelector('#trip-value'); const total = document.querySelector('#example-total');
  const result = document.querySelector('#example-result'); const progress = document.querySelector('.track-progress');
  const threshold = document.querySelector<HTMLElement>('.threshold');
  const update = () => {
    if (!slider) return;
    const count = Number(slider.value);
    const cents = Number(fare?.value || 200);
    const totalCents = count * cents;
    const target = Math.ceil(4800 / cents);
    if (value) value.textContent = String(count);
    if (total) total.textContent = `S$${(totalCents / 100).toFixed(2)}`;
    if (result) result.textContent = totalCents < 4800 ? `${Math.ceil((4800-totalCents)/cents)} more trips would cover the pass.` : totalCents === 4800 ? 'The pass cost is covered.' : `S$${((totalCents-4800)/100).toFixed(2)} above the pass price.`;
    gsap.to(progress, { clipPath: `inset(0 ${100 - count / 36 * 100}% 0 0)`, duration: reduce ? 0 : .12, ease: 'power3.out', overwrite: true });
    const track = document.querySelector<HTMLElement>('.journey-track');
    if (track && threshold) {
      const label = threshold.querySelector('b'); if (label) label.textContent = `Enough: ${target} trips`;
      gsap.to(threshold, { x: 12 + target / 36 * (track.clientWidth - 24) - track.clientWidth * 2 / 3, duration: reduce ? 0 : .25, ease: 'power3.out', overwrite: true });
    }
  };
  slider?.addEventListener('input', update);
  fare?.addEventListener('change', update);
  const resize = new ResizeObserver(update); const track = document.querySelector('.journey-track');
  if (track) resize.observe(track); update();
  dispose = () => { topics?.removeEventListener('click', toggleTopics); slider?.removeEventListener('input', update); fare?.removeEventListener('change', update); resize.disconnect(); if (tick) gsap.ticker.remove(tick); lenis?.destroy(); gsap.killTweensOf([progress, threshold].filter(Boolean)); };
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
