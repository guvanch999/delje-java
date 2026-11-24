-- Catalog schema: categories, products, product images, banners.
-- Base text columns hold Russian content, *_tk columns hold the Turkmen translation.

CREATE TABLE categories (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(255) NOT NULL,
    slug    VARCHAR(255) NOT NULL,
    name_tk VARCHAR(255),
    CONSTRAINT categories_slug_key UNIQUE (slug)
);

CREATE TABLE banners (
    id       SERIAL PRIMARY KEY,
    image    VARCHAR(255) NOT NULL,
    link     VARCHAR(255),
    title    VARCHAR(255),
    active   BOOLEAN NOT NULL DEFAULT TRUE,
    title_tk VARCHAR(255)
);

CREATE TABLE products (
    id             SERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    slug           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          NUMERIC(12, 2) NOT NULL,
    is_top         BOOLEAN NOT NULL DEFAULT FALSE,
    category_id    INTEGER,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    name_tk        VARCHAR(255),
    description_tk TEXT,
    CONSTRAINT products_slug_key UNIQUE (slug),
    CONSTRAINT products_category_id_fkey FOREIGN KEY (category_id)
        REFERENCES categories (id) ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX products_price ON products (price);
CREATE INDEX products_created_at ON products (created_at);
CREATE INDEX products_category_id ON products (category_id);

CREATE TABLE product_images (
    id         SERIAL PRIMARY KEY,
    product_id INTEGER NOT NULL,
    thumb      VARCHAR(255) NOT NULL,
    "full"     VARCHAR(255) NOT NULL,
    sort       INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT product_images_product_id_fkey FOREIGN KEY (product_id)
        REFERENCES products (id) ON UPDATE CASCADE ON DELETE CASCADE
);
