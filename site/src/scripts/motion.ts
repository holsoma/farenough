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
  dispose = () => { topics?.removeEventListener('click', toggleTopics); if (tick) gsap.ticker.remove(tick); lenis?.destroy(); };
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
