let PublicationsPerspective;

(function () {
    if (typeof PublicationsPerspective == 'undefined') {
        PublicationsPerspective = {};
        PublicationsPerspective.iFrameRendered = false;
    }

    PublicationsPerspective.showIFrame = function (id) {
        if (!PublicationsPerspective.iFrameRendered) {
            let iframe = $('#publications-perspective').find('iframe');
            iframe.attr('src', '/importer/');
            PublicationsPerspective.iFrameRendered = true;
        }
    };

})();
