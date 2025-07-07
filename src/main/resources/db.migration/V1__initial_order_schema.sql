CREATE TABLE public.orders (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	user_id uuid NULL,
	order_date timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	status varchar(50) NOT NULL,
	total_amount numeric(10, 2) NOT NULL,
	shipping_address_id uuid NOT NULL,
	billing_address_id uuid NOT NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT order_pkey PRIMARY KEY (id)
);

ALTER TABLE public.orders ADD CONSTRAINT order_billing_address_id_fkey FOREIGN KEY (billing_address_id) REFERENCES public.order_address(id);
ALTER TABLE public.orders ADD CONSTRAINT order_shipping_address_id_fkey FOREIGN KEY (shipping_address_id) REFERENCES public.order_address(id);
ALTER TABLE public.orders ADD CONSTRAINT order_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE SET NULL;

CREATE TABLE public.order_address (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	street varchar(255) NOT NULL,
	"number" varchar(20) NULL,
	apartment varchar(20) NULL,
	city varchar(100) NOT NULL,
	state varchar(100) NULL,
	postal_code varchar(20) NULL,
	country varchar(50) NOT NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT order_address_pkey PRIMARY KEY (id)
);

CREATE TABLE public.order_item (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	order_id uuid NOT NULL,
	product_id uuid NOT NULL,
	quantity int4 NOT NULL,
	unit_price numeric(10, 2) NOT NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT order_item_pkey PRIMARY KEY (id)
);

ALTER TABLE public.order_item ADD CONSTRAINT order_item_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;
ALTER TABLE public.order_item ADD CONSTRAINT order_item_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;

CREATE TABLE public.shipment (
	id uuid DEFAULT gen_random_uuid() NOT NULL,
	order_id uuid NOT NULL,
	shipping_date timestamp NULL,
	tracking_number varchar(255) NULL,
	carrier varchar(255) NULL,
	status varchar(50) NULL,
	created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
	CONSTRAINT shipment_pkey PRIMARY KEY (id)
);

ALTER TABLE public.shipment ADD CONSTRAINT shipment_order_id_fkey FOREIGN KEY (order_id) REFERENCES public.orders(id) ON DELETE CASCADE;