let slideshowInterval;

function displayImage(url) {
    clearInterval(slideshowInterval);
    document.getElementById('swiperWrapper').innerHTML = `<div class="swiper-slide"><img src="${url}" /></div>`;
}

function startSlideshow(images) {
    let index = 0;
    slideshowInterval = setInterval(() => {
        index = (index + 1) % images.length;
        displayImage(images[index].url);
    }, 3000);
}

window.onload = function() {
    fetch('/gallery')
        .then(response => response.json())
        .then(data => {
            if (data.length > 0) {
                const swiperWrapper = document.getElementById('swiperWrapper');
                data.forEach(image => {
                    const slide = document.createElement('div');
                    slide.className = 'swiper-slide';
                    slide.innerHTML = `<img src="${image.url}" />`;
                    swiperWrapper.appendChild(slide);
                });

                new Swiper('.swiper', {
                    navigation: {
                        nextEl: '.swiper-button-next',
                        prevEl: '.swiper-button-prev',
                    },
                });
            }
        })
        .catch(error => console.error('Error fetching gallery:', error));
};
