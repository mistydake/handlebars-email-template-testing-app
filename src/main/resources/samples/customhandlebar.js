// Custom helpers for the sample template.
// Register each one with Handlebars.registerHelper(name, function (...args, options) { ... }).
// The LAST argument is always the Handlebars "options" object.
// Note: dateFormat, lower and upper are built into the app, so they are not defined here.

function toNumber(v) {
    var n = Number(v);
    return isNaN(n) ? 0 : n;
}

function sumLines(items) {
    var total = 0;
    if (!items) { return total; }
    for (var i = 0; i < items.length; i++) {
        total += toNumber(items[i].price) * toNumber(items[i].quantity);
    }
    return total;
}

Handlebars.registerHelper('formatCurrency', function (value, currency, options) {
    var symbols = { USD: '$', EUR: '€', GBP: '£' };
    var code = (typeof currency === 'string' && currency) ? currency : 'USD';
    var symbol = symbols[code] || (code + ' ');
    var parts = toNumber(value).toFixed(2).split('.');
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    return symbol + parts.join('.');
});

Handlebars.registerHelper('multiply', function (a, b, options) {
    return toNumber(a) * toNumber(b);
});

Handlebars.registerHelper('sumLineTotals', function (items, options) {
    return sumLines(items);
});

Handlebars.registerHelper('taxAmount', function (items, rate, options) {
    return sumLines(items) * toNumber(rate);
});

Handlebars.registerHelper('orderTotal', function (items, rate, options) {
    var subtotal = sumLines(items);
    return subtotal + subtotal * toNumber(rate);
});

Handlebars.registerHelper('percent', function (value, options) {
    return (toNumber(value) * 100).toFixed(1).replace(/\.0$/, '') + '%';
});

Handlebars.registerHelper('count', function (list, options) {
    return list ? list.length : 0;
});

Handlebars.registerHelper('pluralize', function (n, one, many, options) {
    return toNumber(n) === 1 ? one : many;
});

// Block helper: {{#ifEquals a b}} ... {{else}} ... {{/ifEquals}}
Handlebars.registerHelper('ifEquals', function (a, b, options) {
    return String(a) === String(b) ? options.fn(this) : options.inverse(this);
});
