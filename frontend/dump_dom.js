const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000', { waitUntil: 'networkidle0' });

  // Get the height of the body and main elements
  const heights = await page.evaluate(() => {
    const main = document.querySelector('.app-main');
    const hero = document.querySelector('.hero-section');
    const items = document.querySelectorAll('*');
    let maxH = 0;
    let maxEl = '';
    
    for(let el of items) {
       if(el.clientHeight > 5000) {
          maxEl += el.tagName + '.' + el.className + ' h:' + el.clientHeight + '\n';
       }
    }
    
    return {
      bodyHeight: document.body.clientHeight,
      mainHeight: main ? main.clientHeight : null,
      giantElements: maxEl,
      mainHTML: main ? main.innerHTML.substring(0, 1000) : 'no main'
    };
  });

  console.log(JSON.stringify(heights, null, 2));
  await browser.close();
})();
