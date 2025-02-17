let FeedbackPerspective;

(function () {
    if (typeof FeedbackPerspective == 'undefined') {
        FeedbackPerspective = {};
        FeedbackPerspective.iFrameRendered = false;
    }

    FeedbackPerspective.showIFrame = function (id) {
        if (!FeedbackPerspective.iFrameRendered) {
            let iframe = $('#feedback-perspective').find('iframe');
            iframe.attr('src', '/feedback/');
            FeedbackPerspective.iFrameRendered = true;
        }
    };

    window.addEventListener('message', function (event) {
        if (typeof event.data !== 'string') {
            return;
        }
        let link = $('.feedback-content-item-link');
        let eventData = JSON.parse(event.data);
        link.attr('data-uuid', eventData.uuid);
        link.attr('data-path', eventData.path);
        link.click();
    });

})();
