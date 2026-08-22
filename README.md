# Phase 2 — Database + JPA

## What's in here

```
src/main/java/com/ecommerce/
├── entity/
│   ├── enums/          RoleName, EntityStatus, OrderStatus, PaymentStatus,
│   │                   PaymentMethod, DiscountType, CouponStatus
│   ├── BaseAuditEntity.java   (shared createdAt/updatedAt for entities that had both)
│   ├── Role, User, Address, Category, SubCategory, Brand,
│   ├── Product, ProductImage, Cart, CartItem, Wishlist,
│   ├── Order, OrderItem, Payment, Coupon, CouponUsage,
│   └── Review, Notification
└── repository/
    └── one Spring Data JpaRepository per entity

src/main/resources/
├── db/migration/V1__init_schema.sql   (Flyway migration — fixed version of your script)
└── application.yml                    (datasource + JPA + Flyway config)
```

## Fixes applied to your original SQL

1. **Removed `SELECT * FROM roles;`** before `CREATE TABLE roles` — this would have failed the script outright since the table didn't exist yet.
2. **Added `addresses` table** and switched `orders.shipping_address` from a raw `TEXT` blob to `shipping_address_id BIGINT` FK — lets users save/reuse multiple addresses.
3. **`coupons.discount_type`** now has `CHECK (discount_type IN ('PERCENTAGE','FIXED'))`.
4. **`coupons.max_usage`** now defaults to `NULL` (= unlimited) instead of `0` (ambiguous).
5. **Added `coupon_id` FK on `orders`** + a `coupon_usages` table to track per-user redemptions and enforce usage limits.
6. **`sub_categories.name`** is now unique *per category* (`UNIQUE(category_id, name)`) instead of having no constraint at all.

## Required Maven dependencies (add to your Phase 1 `pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## Why Flyway instead of `ddl-auto=update`

`application.yml` sets `ddl-auto: validate`, meaning Hibernate checks your
entities match the DB schema but never mutates it. Flyway (`V1__init_schema.sql`)
is the single source of truth for the schema — this avoids Hibernate silently
altering columns/constraints as you add more entities in later phases, and gives
you a real migration history as the schema evolves (`V2__...`, `V3__...`, etc).

## Notes / things to decide in later phases

- ~~**`User.password`** is a plain `String` here — Phase 6 (Authentication) will
  need to ensure this is only ever set via a BCrypt-hashed value, never persisted raw.~~
  **Resolved in Phase 6**: `AuthServiceImpl.register` always calls `passwordEncoder.encode(...)`
  before persisting; nothing else writes to `User.password`.
- **`Role`** is seeded with `ADMIN`/`USER` via the migration `INSERT`. If you need
  more granular roles later, add a new migration (`V2__add_roles.sql`) — don't edit `V1`.
- **`Coupon` ↔ `Order`** relationship assumes a coupon applies to the whole order,
  not individual line items. Flag this now if that's not what you intended.
- Validation annotations (`@NotBlank`, `@Email`, `@Min`, etc.) are intentionally
  **not** on these entities — they belong on request DTOs instead, and in fact
  every request DTO from Phase 3 onward already carries them (e.g.
  `CategoryRequest.@NotBlank` name). Phase 15 (Exception + Validation) doesn't
  add these annotations retroactively — it adds the `MethodArgumentNotValidException`
  handler that actually turns a validation failure into a useful field-level
  error response instead of Spring's default one.

---

# Phase 3 — Category

## What's new

```
src/main/java/com/ecommerce/
├── dto/category/
│   ├── CategoryRequest.java       (name, description — validated)
│   ├── CategoryResponse.java      (id, name, description, status, createdAt, subCategories)
│   ├── SubCategoryRequest.java    (categoryId, name, description — validated)
│   └── SubCategoryResponse.java
├── mapper/
│   └── CategoryMapper.java        (manual entity <-> DTO mapping, no MapStruct dependency)
├── service/
│   ├── CategoryService.java
│   └── impl/CategoryServiceImpl.java
├── controller/
│   └── CategoryController.java
└── exception/
    ├── ResourceNotFoundException.java
    ├── DuplicateResourceException.java
    └── GlobalExceptionHandler.java   (minimal — 404/409 only, expands in Phase 15)
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/categories` | Create a category |
| GET | `/api/categories` | List all categories (with nested sub-categories) |
| GET | `/api/categories/{id}` | Get one category |
| PUT | `/api/categories/{id}` | Update a category |
| DELETE | `/api/categories/{id}` | Delete a category |
| POST | `/api/subcategories` | Create a sub-category (`categoryId` in body) |
| GET | `/api/categories/{categoryId}/subcategories` | List sub-categories of a category |
| GET | `/api/subcategories/{id}` | Get one sub-category |
| PUT | `/api/subcategories/{id}` | Update a sub-category |
| DELETE | `/api/subcategories/{id}` | Delete a sub-category |

## New Maven dependency required

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
`spring-boot-starter-web` is needed for `@RestController`; `spring-boot-starter-validation`
is needed for the `@NotBlank`/`@Size`/`@NotNull` annotations on the request DTOs to
actually be enforced when `@Valid` is used in the controller.

## Design decisions worth flagging

- ~~**Minimal exception handling included now, not deferred entirely to Phase 15.**~~
  **Resolved in Phase 15**: `GlobalExceptionHandler` was consolidated onto a shared
  `ErrorResponse` DTO with full coverage — field-level validation errors,
  malformed bodies, type mismatches, wrong HTTP methods, unmapped routes, and a
  safety-net handler for any DB constraint violation that reaches it unhandled.
- ~~**Deleting a category with existing products will currently fail with a raw DB
  constraint error** (500)~~ **Resolved in Phase 15**: `deleteCategory` now
  pre-checks `productRepository.existsByCategoryId(id)` and throws
  `ResourceInUseException` → a clean 409 with an actionable message, before
  ever reaching the DB constraint.
- **`CategoryMapper` is a hand-written static mapper**, not MapStruct. Given the
  entity count in this project, MapStruct would save boilerplate — happy to switch
  if you'd rather generate mappers than hand-write ~15 more of these.
- **Sub-category name uniqueness is still not pre-checked** the way category
  names are — a duplicate sub-category name still isn't caught by a specific
  service-layer check. **Partially improved by Phase 15**, though: it no longer
  surfaces as a raw 500 — the new generic `DataIntegrityViolationException`
  safety-net handler now catches it and returns 409. The message is generic
  ("referenced by other data"), not the more accurate "a sub-category with this
  name already exists in this category" a dedicated pre-check would give. Worth
  a real fix if sub-category name collisions are likely in practice — flag if
  you want it patched now rather than left on the generic fallback.

---

# Phase 4 — Brand

## What's new

```
src/main/java/com/ecommerce/
├── dto/brand/
│   ├── BrandRequest.java    (name, description, logo — validated)
│   └── BrandResponse.java
├── mapper/
│   └── BrandMapper.java
├── service/
│   ├── BrandService.java
│   └── impl/BrandServiceImpl.java
└── controller/
    └── BrandController.java
```

Simpler than Category — no parent/child hierarchy, so it's a straight CRUD module
following the exact same pattern (duplicate-name check on create/update, 404 via
`ResourceNotFoundException`, 409 via `DuplicateResourceException`).

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/brands` | Create a brand |
| GET | `/api/brands` | List all brands |
| GET | `/api/brands/{id}` | Get one brand |
| PUT | `/api/brands/{id}` | Update a brand |
| DELETE | `/api/brands/{id}` | Delete a brand |

## Same caveat as Category

~~Deleting a brand still referenced by products isn't caught at the service layer —
`fk_product_brand` has no cascade/set-null, so it'll surface as a raw DB constraint
error (500) until Phase 15 adds proper FK-violation translation. Consistent with the
Phase 3 note; will fix both at once rather than patching ad hoc.~~

**Resolved in Phase 15**: `deleteBrand` now pre-checks `productRepository.existsByBrandId(id)`
and throws `ResourceInUseException` → a clean 409, matching the fix applied to Category.

---

# Phase 5 — Product

## What's new

```
src/main/java/com/ecommerce/
├── dto/common/
│   └── PagedResponse.java             (generic page wrapper: content, page, size, totalElements, totalPages, last)
├── dto/product/
│   ├── ProductRequest.java            (categoryId, subCategoryId?, brandId?, name, description, price, stock, sku)
│   ├── ProductResponse.java           (flattened category/subCategory/brand names + nested images)
│   ├── ProductImageRequest.java       (imageUrl, isPrimary)
│   ├── ProductImageResponse.java
│   └── ProductFilterRequest.java      (all-optional filter fields, bound from query params)
├── repository/specification/
│   └── ProductSpecification.java      (dynamic JPA Specification for filtering)
├── mapper/
│   └── ProductMapper.java
├── service/
│   ├── ProductService.java
│   └── impl/ProductServiceImpl.java
├── controller/
│   └── ProductController.java
└── exception/
    └── InvalidRequestException.java   (new — 400, for semantically-invalid input; see below)
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/products` | Create a product |
| GET | `/api/products` | Paginated + filtered listing (see query params below) |
| GET | `/api/products/{id}` | Get one product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |
| PATCH | `/api/products/{id}/stock?quantity=N` | Set stock to an absolute value |
| POST | `/api/products/{id}/images` | Add an image |
| GET | `/api/products/{id}/images` | List a product's images |
| DELETE | `/api/products/{id}/images/{imageId}` | Remove an image |
| PATCH | `/api/products/{id}/images/{imageId}/primary` | Set an image as primary (unsets any other) |

### Listing query params (all optional, combinable)

`name`, `categoryId`, `subCategoryId`, `brandId`, `minPrice`, `maxPrice`, `status`
(`ACTIVE`/`INACTIVE`), `inStockOnly` (boolean) — plus standard Spring pagination params
`page`, `size`, `sort` (e.g. `sort=price,desc`). Default page size is 20.

Example: `GET /api/products?categoryId=3&minPrice=10&maxPrice=100&sort=price,asc`

## Design decisions worth flagging

- **Filtering uses JPA `Specification`** (`ProductSpecification`), which is why
  `ProductRepository` already extended `JpaSpecificationExecutor<Product>` back in
  Phase 2 — that wasn't used until now. This scales cleanly as more filter fields
  get added later (e.g. rating from Phase 13) without new repository methods.
- **`ProductRequest.subCategoryId` is validated against `categoryId`** in the service
  layer (`resolveSubCategory`) — passing a sub-category that belongs to a *different*
  category throws `InvalidRequestException` → 400, not a silent mismatch.
- **New `InvalidRequestException` → 400**, distinct from bean-validation errors
  (which need `spring-boot-starter-validation` + `@Valid`, already added in Phase 3).
  Used for semantically-invalid combinations that annotations alone can't catch
  (sub-category/category mismatch, image not belonging to the given product, negative
  stock on the PATCH endpoint).
- **`updateStock` replaces the stock value** rather than incrementing/decrementing it.
  Cart/Order (Phases 8 & 10) will need atomic increment/decrement instead (e.g.
  `stock = stock - :qty` at the query level) to avoid race conditions under concurrent
  checkouts — flagging now so it's not forgotten; this endpoint alone isn't sufficient
  once real order flows exist.
- **Primary image is enforced in the service layer**, not the DB — `clearExistingPrimaryImage`
  unsets any other primary image before setting a new one. There's no DB-level
  constraint guaranteeing "at most one primary image per product"; fine for now given
  all writes go through this service, but worth a partial unique index if this schema
  is ever written to directly.
- ~~**Deleting a product still referenced by cart_items/order_items/reviews/wishlists**
  will surface as a raw DB constraint error (500)~~ **Resolved in Phase 15**,
  and resolved *differently* per reference rather than uniformly, since they're
  not equivalent: `product_images`/`reviews`/`wishlists` already cascade-delete
  at the DB level (no code needed). `order_items` has no cascade and **blocks
  deletion outright** (`ResourceInUseException` → 409, suggesting `status =
  INACTIVE` instead) — order history must never silently lose a line item.
  `cart_items` also has no cascade, but is handled differently again: a cart
  isn't a historical record, so `deleteProduct` now calls
  `cartItemRepository.deleteByProductId(id)` to silently clean up stray cart
  entries *before* deleting the product, rather than blocking the delete.
  Soft-delete (a status flip instead of ever hard-deleting) was considered as
  the simpler, uniform alternative and deliberately not adopted — the
  per-reference handling above was judged more correct than one blanket rule.

---

# Phase 6 — Authentication

## What's new

```
src/main/java/com/ecommerce/
├── security/
│   ├── JwtProperties.java          (binds app.jwt.* from application.yml)
│   ├── JwtService.java             (generates/validates HS256 access tokens)
│   ├── CustomUserDetails.java      (adapts User -> Spring Security UserDetails)
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java (reads Bearer token, populates SecurityContext)
│   ├── RefreshTokenService.java    (opaque refresh tokens, stored server-side, rotated on use)
│   ├── SecurityResponseHandlers.java (JSON 401/403 bodies, matching GlobalExceptionHandler's shape)
│   └── SecurityUtils.java          (SecurityUtils.getCurrentUserId() for later phases)
├── config/
│   └── SecurityConfig.java         (filter chain, CORS, public/protected route rules)
├── dto/auth/
│   ├── RegisterRequest.java / LoginRequest.java / RefreshTokenRequest.java
│   └── AuthResponse.java
├── entity/
│   └── RefreshToken.java
├── repository/
│   └── RefreshTokenRepository.java
├── service/
│   ├── AuthService.java
│   └── impl/AuthServiceImpl.java
├── controller/
│   └── AuthController.java
└── exception/
    ├── InvalidCredentialsException.java  (401 — bad login / bad refresh token)
    └── UnauthorizedException.java        (401 — no authenticated user in context)

src/main/resources/db/migration/
└── V2__add_refresh_tokens.sql
```

## Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create account, auto-assigned `USER` role, returns tokens |
| POST | `/api/auth/login` | Public | Authenticate, returns tokens |
| POST | `/api/auth/refresh` | Public | Exchange a valid refresh token for a new token pair (rotates) |
| POST | `/api/auth/logout` | Public | Revoke a refresh token |

All other endpoints now require `Authorization: Bearer <accessToken>` **except**
`GET` on `/api/categories/**`, `/api/subcategories/**`, `/api/brands/**`,
`/api/products/**` (public catalog browsing). Writes (POST/PUT/PATCH/DELETE) on
those same paths require the `ADMIN` role.

## How the token flow works

- **Access token**: short-lived (15 min default) JWT, stateless, carries `userId` and
  `roles` claims, verified on every request by `JwtAuthenticationFilter`.
- **Refresh token**: opaque random 64-byte value (not a JWT), stored in the
  `refresh_tokens` table so it can be revoked server-side. Long-lived (7 days default).
- **Rotation**: every `/auth/refresh` call revokes the token it was given and issues a
  brand-new one — a leaked-but-unused refresh token becomes worthless the moment the
  legitimate client refreshes.
- **Single active session per user**: issuing a new refresh token (login or refresh)
  revokes any other outstanding ones for that user. This means logging in on a second
  device currently invalidates the first device's refresh token — flag if you want
  multi-device sessions instead (straightforward change: drop the
  `revokeAllForUser` call in `RefreshTokenService.issueToken`).

## Bugs found and fixed during review

Two issues would have broken the build/runtime and are already corrected in the files above:

1. **`GlobalExceptionHandler` had two `handleBadCredentials(BadCredentialsException)`
   methods** (one via a normal import, one fully-qualified) — same erasure, wouldn't
   compile. Removed the duplicate and cleaned up the other fully-qualified handlers
   (`AccessDeniedException`, `MethodArgumentNotValidException`) to use proper imports.
2. **The default JWT secret in `application.yml` wasn't valid Base64** (contained
   hyphens), but `JwtService` Base64-decodes it to build the signing key — this would
   throw on the very first token generation. Replaced with a proper random Base64
   value. Still dev-only — override `APP_JWT_SECRET` before deploying anywhere real.

## Design decisions worth flagging

- **`spring-boot-starter-security` + `jjwt` (0.12.6) were already added to `pom.xml`**
  in the Phase 1/2 build file, anticipating this phase.
- **JwtService uses jjwt's pre-0.12 builder API** (`Jwts.builder().setSubject()`,
  `Jwts.parserBuilder()`) which still works under jjwt 0.12.6 but is deprecated in
  favor of `.subject()` / `Jwts.parser()`. Functionally fine — flag if you'd like it
  modernized to the non-deprecated API now rather than later.
- **`SecurityResponseHandlers`** exists because Spring Security's 401/403 responses
  fire *before* `@RestControllerAdvice` gets a chance to handle them — without this,
  unauthenticated/forbidden requests would get Spring's default HTML error page
  instead of the same JSON shape as every other error response.
- **Registering a user does not create a `Cart`** yet, even though `User` ↔ `Cart` is
  a one-to-one relationship. Decide in Phase 8 whether to create the cart eagerly on
  registration (in `AuthServiceImpl.register`) or lazily on first cart access — both
  are reasonable, but it needs to happen somewhere.
- **CORS is wide open** (`allowedOriginPatterns("*")`) for local dev / Phase 19 —
  must be tightened to the real frontend origin(s) before any real deployment.
- **Password policy** requires 8–72 chars with at least one letter and one digit
  (72 = BCrypt's hard input limit, enforced explicitly rather than silently truncated).

---

# Phase 7 — User & Role

## What's new

```
src/main/java/com/ecommerce/
├── dto/user/
│   ├── UserResponse.java             (id, name, email, phone, status, roles, timestamps)
│   ├── UpdateProfileRequest.java     (name, phone — no email/password)
│   └── ChangePasswordRequest.java    (currentPassword, newPassword — validated)
├── dto/address/
│   ├── AddressRequest.java
│   └── AddressResponse.java
├── dto/admin/
│   ├── AdminUserFilterRequest.java   (search, status, role — query params)
│   ├── UpdateUserStatusRequest.java
│   └── AssignRolesRequest.java       (replaces the full role set, not additive)
├── dto/role/
│   └── RoleResponse.java
├── mapper/
│   ├── UserMapper.java
│   ├── AddressMapper.java
│   └── RoleMapper.java
├── repository/specification/
│   └── UserSpecification.java        (admin search/filter, same pattern as ProductSpecification)
├── service/
│   ├── UserService.java + impl        (self-service: profile, password, addresses)
│   ├── AdminUserService.java + impl   (admin: list/view/status/roles/delete on any user)
│   └── RoleService.java + impl        (read-only role listing)
└── controller/
    ├── UserController.java            (/api/users/me/**)
    ├── AdminUserController.java       (/api/admin/users/**)
    └── AdminRoleController.java       (/api/admin/roles)
```

Two repositories gained new methods: `UserRepository` now extends `JpaSpecificationExecutor<User>`
(for admin search/filter), and `AddressRepository` gained `findByIdAndUserId` (ownership check).

## Endpoints

### Self-service (`/api/users/me/**`) — any authenticated user, acts on themselves only

| Method | Path | Description |
|---|---|---|
| GET | `/api/users/me` | Get my profile |
| PUT | `/api/users/me` | Update my name/phone |
| PATCH | `/api/users/me/password` | Change my password (requires current password) |
| GET | `/api/users/me/addresses` | List my addresses |
| POST | `/api/users/me/addresses` | Add an address |
| PUT | `/api/users/me/addresses/{addressId}` | Update an address |
| DELETE | `/api/users/me/addresses/{addressId}` | Delete an address |
| PATCH | `/api/users/me/addresses/{addressId}/default` | Set as default address |

### Admin (`/api/admin/**`) — ROLE_ADMIN only, enforced in `SecurityConfig`

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/users` | Paginated user list. Query params: `search`, `status`, `role` |
| GET | `/api/admin/users/{id}` | Get any user |
| PATCH | `/api/admin/users/{id}/status` | Activate/deactivate a user |
| PUT | `/api/admin/users/{id}/roles` | Replace a user's roles |
| DELETE | `/api/admin/users/{id}` | Delete a user |
| GET | `/api/admin/roles` | List available roles |

## Design decisions worth flagging

- **Admins can't act on their own account through the admin endpoints** —
  `AdminUserServiceImpl` blocks self-deactivation, self-role-change, and
  self-deletion via `assertNotActingOnSelf`, returning 400. Without this, an
  admin could accidentally lock themselves out (strip their own ADMIN role,
  deactivate their own account) with no way back in short of a DB fix. They can
  still edit their own profile via the self-service endpoints.
- **Email is not updatable via `UpdateProfileRequest`.** Changing a login
  email is security-sensitive (ties into account recovery, notifications,
  potentially re-verification) and deliberately out of scope here — add a
  dedicated `/email-change` flow later if needed rather than folding it into
  the general profile update.
- **`AssignRolesRequest` replaces the full role set, not additive.** `PUT
  .../roles` with `{"roles": ["ADMIN"]}` removes USER if it was the only other
  role present. This matches the semantics of `PUT` (full replacement) — flag
  if you'd rather have separate add/remove-role endpoints instead.
- **Password change doesn't revoke existing sessions.** After changing their
  password, a user's other logged-in devices stay authenticated until their
  access token naturally expires (≤15 min) — refresh tokens aren't revoked.
  Worth reconsidering once this matters for your threat model; the fix is one
  line (`refreshTokenService.revokeAllForUser(...)` in `changePassword`).
- **`findOwnedAddressOrThrow` returns 404, not 403,** when an address exists
  but belongs to a different user — this avoids confirming to a caller that a
  given address ID exists at all, which is the safer default for this kind of
  ownership check.
- ~~**Deleting a user with existing orders will surface as a raw DB constraint
  error** (500) once Phase 10 exists~~ **Resolved in Phase 15**: `deleteUser`
  now pre-checks `orderRepository.existsByUserId(id)` and throws
  `ResourceInUseException` → a clean 409, same pattern as Category/Brand/Product.
  The stronger option raised here — removing hard-delete from the API
  entirely and enforcing soft-delete-only for users — was considered and not
  adopted; the pre-check-and-block pattern was kept for consistency with every
  other entity in the project rather than making User a special case. Revisit
  if that consistency turns out to matter less than the stronger guarantee.

---

# Phase 8 — Cart

## What's new

```
src/main/java/com/ecommerce/
├── dto/cart/
│   ├── CartItemRequest.java        (productId, quantity)
│   ├── UpdateCartItemRequest.java  (quantity — absolute, not delta)
│   ├── CartItemResponse.java       (live product name/image/price + insufficientStock flag)
│   └── CartResponse.java           (items, totalItems, totalAmount, updatedAt)
├── mapper/
│   └── CartMapper.java
├── service/
│   ├── CartService.java
│   └── impl/CartServiceImpl.java
└── controller/
    └── CartController.java         (/api/cart/**)
```

`CartItemRepository` gained `findByIdAndCartId` (ownership check, same pattern as
`AddressRepository.findByIdAndUserId` from Phase 7).

## Endpoints (`/api/cart/**`) — any authenticated user, acts on themselves only

| Method | Path | Description |
|---|---|---|
| GET | `/api/cart` | Get my cart (empty shell if none exists yet) |
| POST | `/api/cart/items` | Add a product (merges quantity if already in cart) |
| PUT | `/api/cart/items/{itemId}` | Set an item's quantity to an absolute value |
| DELETE | `/api/cart/items/{itemId}` | Remove one item |
| DELETE | `/api/cart` | Clear the entire cart |

## Design decisions worth flagging

- **Lazy cart creation, resolved from the Phase 7 open question.** A `Cart` row
  is only created on the first `POST /api/cart/items` call. `GET /api/cart` and
  `DELETE /api/cart` on a user with no cart yet return an empty `CartResponse`
  (`id: null`, empty items) rather than 404 or creating a row just to view it.
- **Pricing is live, not a snapshot.** `CartItemResponse.unitPrice`/`subtotal`
  always reflect the product's *current* price at read time — if a product's
  price changes while it's sitting in someone's cart, the cart total updates
  automatically next time it's fetched. This is deliberate for a cart (unlike
  an Order, which will need to snapshot price at checkout in Phase 10 — the
  cart is not the source of truth for what a customer is ultimately charged).
- **Adding beyond available stock is allowed, not blocked.** `addItem` only
  rejects products that are `INACTIVE`; it does not compare `quantity` against
  `product.stock`. Instead, `CartItemResponse.insufficientStock` surfaces the
  problem so the frontend can warn the user, while the hard stock check and
  atomic decrement happen at checkout (Phase 10/11) where it actually matters
  for correctness. Flag if you'd rather cap `addItem`/`updateItem` at available
  stock immediately instead.
- **`addItem` merges quantities** if the product is already in the cart
  (`existing.quantity + request.quantity`), matching typical cart UX ("Add to
  cart" twice = 2 total, not an error or a second line item). `updateItem` by
  contrast sets an *absolute* quantity — use it for a quantity stepper in the
  cart view itself.
- **No endpoint validates `updateItem`/`addItem` quantity against stock at all**
  right now, per the point above — this is intentional, not an oversight, but
  flagging again since it's the one place in this phase most likely to surprise
  someone expecting a hard cap.

## Notes / things to decide in later phases

- **Order (Phase 10) will need to snapshot `unitPrice` and product details at
  the moment of checkout**, independent of the live-pricing behavior here —
  don't reuse `CartItemResponse` as-is for `OrderItem` construction.
- **Stock is only truly reserved/decremented at checkout**, not when added to
  cart — under concurrent checkouts this needs an atomic
  `UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty`
  (or a pessimistic lock), not a read-then-write from the cart's `insufficientStock`
  check. Flagged already in Phase 5; repeating here since Phase 10 is where it
  actually gets implemented.

---

# Phase 9 — Wishlist

## What's new

```
src/main/java/com/ecommerce/
├── dto/wishlist/
│   └── WishlistItemResponse.java   (flattened product info + outOfStock flag)
├── mapper/
│   └── WishlistMapper.java
├── service/
│   ├── WishlistService.java
│   └── impl/WishlistServiceImpl.java
└── controller/
    └── WishlistController.java     (/api/wishlist/**)
```

`WishlistRepository` gained `existsByUserIdAndProductId` (cheap duplicate/membership
check, avoids fetching the whole entity just to test presence).

## Endpoints (`/api/wishlist/**`) — any authenticated user, acts on themselves only

| Method | Path | Description |
|---|---|---|
| GET | `/api/wishlist` | List my wishlist, newest first |
| POST | `/api/wishlist/{productId}` | Add a product |
| DELETE | `/api/wishlist/{productId}` | Remove a product |
| GET | `/api/wishlist/{productId}/exists` | `{"inWishlist": true/false}` — for a frontend heart icon |
| POST | `/api/wishlist/{productId}/move-to-cart?quantity=1` | Add to cart + remove from wishlist, atomically |

## Design decisions worth flagging

- **Keyed by `productId`, not a wishlist-entry id**, unlike Cart's `itemId`-based
  endpoints. A wishlist is conceptually a toggle on a product ("heart" icon) from
  the frontend's perspective, not a separately-managed line item with its own
  quantity/state — so `POST/DELETE /api/wishlist/{productId}` maps more naturally
  onto that UX than round-tripping a wishlist-entry id first. `WishlistItemResponse.id`
  is still exposed in case you need it for something else, just not required for
  these calls.
- **`moveToCart` delegates to `CartService.addItem`** rather than duplicating its
  active-product check and quantity-merge logic — if the product is inactive,
  `addItem` throws before the wishlist entry is deleted, and since the whole
  method is `@Transactional`, nothing commits (the product stays on the wishlist).
  This is the first cross-module service dependency in the project — the pattern
  (inject one service into another rather than duplicate logic) will likely recur
  once Order/Payment start coordinating in Phase 10/11.
- **No duplicate-add protection needed at the DB level beyond what already
  existed** — Phase 2's `UNIQUE(user_id, product_id)` constraint on `wishlists`
  was already in place; the service just pre-checks it with `existsByUserIdAndProductId`
  to return a clean 409 (`DuplicateResourceException`) instead of a raw constraint
  violation, consistent with every other module.
- **`outOfStock` is a snapshot at read time**, same spirit as Cart's
  `insufficientStock` — the wishlist doesn't block adding out-of-stock products
  (there'd be no reason to), it just tells the frontend whether "add to cart" should
  currently be disabled for that item.

---

# Phase 10 — Order

## What's new

```
src/main/java/com/ecommerce/
├── dto/order/
│   ├── PlaceOrderRequest.java          (addressId, optional couponCode)
│   ├── OrderItemResponse.java          (price is a SNAPSHOT, not live)
│   ├── OrderResponse.java              (items, subtotal, discountAmount, totalAmount, address summary)
│   ├── OrderSummaryResponse.java       (lightweight row for list views)
│   ├── UpdateOrderStatusRequest.java   (admin)
│   └── AdminOrderFilterRequest.java    (status, userId, orderNumber — query params)
├── repository/specification/
│   └── OrderSpecification.java         (admin filtering, same pattern as Product/User)
├── mapper/
│   └── OrderMapper.java
├── service/
│   ├── OrderService.java + impl         (self-service: checkout, history, cancel)
│   ├── AdminOrderService.java + impl    (admin: list/view/status transitions)
│   └── impl/OrderStockCoordinator.java  (package-private; shared atomic stock logic)
├── controller/
│   ├── OrderController.java             (/api/orders/**)
│   └── AdminOrderController.java        (/api/admin/orders/**)
└── exception/
    └── InsufficientStockException.java  (new — 409)
```

`ProductRepository` gained atomic `decrementStock`/`incrementStock` (`@Modifying @Query`,
single-statement `WHERE stock >= :qty`). `OrderRepository` gained `existsByOrderNumber`,
`findByIdAndUserId` (ownership check), and now extends `JpaSpecificationExecutor<Order>`.

## Endpoints

### Self-service (`/api/orders/**`) — any authenticated user, acts on themselves only

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders` | Place an order from the current cart. Body: `{addressId, couponCode?}` |
| GET | `/api/orders` | Paginated order history (lightweight summaries) |
| GET | `/api/orders/{id}` | Full order detail |
| PATCH | `/api/orders/{id}/cancel` | Cancel — only while `PENDING`/`CONFIRMED` |

### Admin (`/api/admin/orders/**`) — `ROLE_ADMIN` only

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/orders` | Paginated, filterable (`status`, `userId`, `orderNumber`) |
| GET | `/api/admin/orders/{id}` | Any order's detail |
| PATCH | `/api/admin/orders/{id}/status` | Transition status (validated against the state machine below) |

## Order status state machine

```
PENDING ──► CONFIRMED ──► SHIPPED ──► DELIVERED ──► REFUNDED
   │             │
   └─────────────┴──► CANCELLED
```

Any transition not drawn above — including skipping a stage or a no-op — is rejected
with 400. `CANCELLED` and `REFUNDED` are terminal. Enforced centrally in
`AdminOrderServiceImpl.ALLOWED_TRANSITIONS`; the self-service cancel endpoint uses
the same rule (`PENDING`/`CONFIRMED` only) but doesn't need the full map since it
only ever targets `CANCELLED`.

## Design decisions worth flagging

- **Answering the Phase 9 open question: `placeOrder` requires an existing
  `addressId`** from the caller's own address book (Phase 7), not an inline
  address — matches the `orders.shipping_address_id` FK that's existed since
  Phase 2. If you need guest/inline checkout later, that's a materially
  different flow (an address with no owning user, or a throwaway one) — flag
  if that's coming up soon rather than retrofitting it.
- **Stock is decremented atomically, not read-then-written.** `decrementStock`
  is a single `UPDATE ... WHERE stock >= :qty` — the availability check and the
  write happen in the same statement, so two concurrent checkouts racing for
  the last unit can't both succeed. A `0`-row update means insufficient stock,
  surfaced as `InsufficientStockException` → 409. This was flagged as a gap
  back in Phase 5 and Phase 8; it's resolved here.
- **Order items snapshot price at checkout**, deliberately diverging from
  Cart's live-pricing behavior (Phase 8). `OrderItemResponse.price` will never
  change even if the product's catalog price does later — this is what makes
  order history and invoicing trustworthy.
- **Coupon *redemption* is implemented here; coupon *management* is still
  Phase 12.** Since `coupon_id`, `coupon_usages`, `Coupon`, `CouponRepository`,
  etc. already existed from Phase 2, leaving them completely unwired until
  Phase 12 would mean revisiting this checkout flow later anyway — so
  `placeOrder` accepts an optional `couponCode` and validates: active status,
  date window, usage limit, minimum order amount, and one-use-per-user (via
  `coupon_usages`'s existing unique constraint, pre-checked here for a clean
  400 instead of a raw DB error). What's still deferred to Phase 12: admin
  CRUD for coupons (create/deactivate/list codes), bulk code generation, and
  any coupon analytics.
- **`OrderStockCoordinator` is package-private**, injected into both
  `OrderServiceImpl` and `AdminOrderServiceImpl` — the first *implementation
  detail* (not a public service) shared across two service classes in this
  project. It centralizes the decrement/restore logic so cancellation refunds
  stock identically whether triggered by the customer or an admin.
- **`getMyOrders` paginates in memory** (`findByUserId` then sublist) rather
  than a `Pageable`-aware repository query — deliberately simple for now since
  per-user order counts are small; flagged in a code comment as the thing to
  swap to a proper paged query if that assumption stops holding.
- ~~**No `Payment` is created by `placeOrder`.** The order is left in `PENDING`
  with no payment row at all — Phase 11 owns creating the `Payment` and is
  what's actually expected to move the order to `CONFIRMED` on success. Right
  now nothing advances a `PENDING` order to `CONFIRMED` except the admin
  status endpoint, which is a manual stand-in until Payment exists.~~
  **Resolved in Phase 11**: a successful `processPayment` now drives
  `PENDING → CONFIRMED` automatically.

## Notes / things to decide in later phases

- ~~**Phase 11 (Payment) needs to decide whether `CONFIRMED` means "paid"**
  or something looser~~ — see the resolved note directly above; this was the
  same open question, answered by Phase 11.
- **Refunding stock on `DELIVERED → REFUNDED`** assumes a physical return —
  worth confirming that's actually the intended business rule (vs. a refund
  that doesn't restock, e.g. for a damaged/lost item) before Phase 11 wires up
  real payment refunds against this transition.
- **No partial cancellation/refund** (per-item) — cancellation and refund are
  whole-order operations only. Flag if line-item-level returns are needed;
  that's a materially bigger change to `OrderItem` (would need its own status).

---

# Phase 11 — Payment

## What's new

```
src/main/java/com/ecommerce/
├── gateway/
│   ├── PaymentGatewayService.java          (interface — charge + refund)
│   ├── SimulatedPaymentGatewayService.java (dev/test impl, no real network calls)
│   └── GatewayChargeResult.java
├── dto/payment/
│   ├── ProcessPaymentRequest.java          (paymentMethod, simulateFailure)
│   ├── PaymentResponse.java
│   └── AdminPaymentFilterRequest.java      (status, paymentMethod, orderId)
├── mapper/
│   └── PaymentMapper.java
├── repository/specification/
│   └── PaymentSpecification.java
├── service/
│   ├── PaymentService.java + impl               (self-service: pay, view)
│   ├── AdminPaymentService.java + impl           (admin: read-only listing)
│   ├── impl/PaymentRefundCoordinator.java        (package-private; keeps Payment in sync with Order)
├── controller/
│   ├── PaymentController.java                    (/api/orders/{orderId}/payment)
│   └── AdminPaymentController.java               (/api/admin/payments/**, read-only)
└── db/migration/
    └── V3__add_payment_failure_reason.sql
```

`PaymentRepository` now extends `JpaSpecificationExecutor<Payment>`. `Payment` gained
a `failureReason` column (V3 migration) so a `FAILED` row is a useful audit trail,
not just a bare status.

## Endpoints

### Self-service (`/api/orders/{orderId}/payment`) — order must belong to the caller

| Method | Path | Description |
|---|---|---|
| POST | `/api/orders/{orderId}/payment` | Charge the order's total. Body: `{paymentMethod, simulateFailure?}` |
| GET | `/api/orders/{orderId}/payment` | View the payment for one of my orders |

### Admin (`/api/admin/payments/**`) — `ROLE_ADMIN`, read-only

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/payments` | Paginated, filterable (`status`, `paymentMethod`, `orderId`) |
| GET | `/api/admin/payments/{id}` | Any payment's detail |

No refund endpoint here — see below.

## Design decisions worth flagging

- **Resolves the two open questions from the end of Phase 10.** A successful
  `processPayment` transitions the order `PENDING → CONFIRMED` by calling
  `AdminOrderService.updateOrderStatus` internally — reusing the exact same
  state-machine validation an admin's manual transition would go through,
  rather than setting the status directly. A gateway decline is **not an
  exception** — it's a normal `FAILED` payment response, and the order
  deliberately stays `PENDING` so the customer can retry with a different
  method.
- **Simulated gateway, swappable without touching `PaymentServiceImpl`.**
  `PaymentGatewayService` is the seam — `SimulatedPaymentGatewayService`
  deterministically succeeds unless `simulateFailure: true` is passed (no
  hidden randomness, so demos/tests are repeatable). Replacing it with a real
  Stripe/PayPal/Razorpay adapter later means adding one new `@Service`
  implementing the interface and wiring it in (e.g. via `@ConditionalOnProperty`),
  with zero changes anywhere else. **`simulateFailure` on `ProcessPaymentRequest`
  is a dev-only escape hatch and must be dropped (or ignored) once a real
  gateway replaces the simulator** — a real integration's success/failure comes
  from the actual charge response, never from client input.
- **A gap from Phase 10 is closed here: cancelling/refunding an order now
  keeps `Payment` in sync.** Phase 10 built order cancellation before `Payment`
  existed, so it only restored stock — a paid order that got cancelled would
  leave its `Payment` row stuck at `SUCCESS` forever, silently wrong. `PaymentRefundCoordinator`
  (same package-private, dual-injected pattern as `OrderStockCoordinator`) is
  now called from both `OrderServiceImpl.cancelMyOrder` and
  `AdminOrderServiceImpl.updateOrderStatus` whenever the order moves to
  `CANCELLED` or `REFUNDED` — it refunds via the gateway and marks the payment
  `REFUNDED`, or does nothing if the order was never actually paid.
- **No standalone admin "refund" endpoint, on purpose.** Since refunding is
  wired as a side effect of the Order status transition (`DELIVERED → REFUNDED`,
  or any `→ CANCELLED`), `Payment` can never drift out of sync with `Order` by
  being editable independently — an admin refunds by transitioning the *order*
  via the existing `/api/admin/orders/{id}/status` endpoint, not by hitting
  Payment directly. `AdminPaymentController` is deliberately read-only.
- **One payment row per order, not per attempt.** `payments.order_id` is
  unique (from Phase 2), so retrying after a `FAILED` charge updates the same
  row rather than inserting a new one — `PaymentResponse.failureReason` reflects
  only the most recent attempt, and a prior failure is overwritten once a retry
  succeeds.
- **A failed *refund* attempt isn't modeled as its own status.** `PaymentRefundCoordinator`
  currently records `REFUNDED` regardless of what the gateway's `refund()` call
  returns — fine for the simulated gateway (always succeeds), but a real
  integration can genuinely fail to refund (e.g. funds already withdrawn by the
  customer). Flagged rather than fixed now since there's no real gateway yet to
  observe an actual failure mode against; worth revisiting once Phase 11 gets a
  real adapter.

## Notes / things to decide in later phases

- **COD (cash on delivery) never touches the gateway for either charge or
  refund** — `charge()` returns instant success with no `transactionId`,
  `refund()` is a no-op returning `true`. This is correct as long as COD really
  means "money changes hands physically, outside this system" — worth
  reconfirming that assumption before Phase 18/19 if COD ever needs real
  reconciliation (e.g. a delivery agent marking cash collected).
- **`ProcessPaymentRequest.simulateFailure`** needs to be stripped from the
  request DTO (or hard-ignored) before this goes anywhere near production —
  flagged above, repeating here since it's the single most important thing to
  remember when a real gateway eventually replaces the simulator.

---

# Phase 12 — Coupon

## What's new

```
src/main/java/com/ecommerce/
├── dto/coupon/
│   ├── CouponRequest.java              (admin create/update)
│   ├── CouponResponse.java
│   ├── BulkGenerateCouponRequest.java  (batch of unique single-use-style codes)
│   ├── ValidateCouponRequest.java / CouponValidationResponse.java  (preview, no redemption)
│   ├── CouponUsageResponse.java        (admin: who redeemed a code, and when)
│   └── AdminCouponFilterRequest.java
├── mapper/
│   └── CouponMapper.java
├── repository/specification/
│   └── CouponSpecification.java
├── service/
│   ├── CouponService.java + impl        (self-service: validate against my cart)
│   ├── AdminCouponService.java + impl   (admin: full CRUD, bulk generate, usage report)
│   └── impl/CouponValidator.java        (package-private; shared redemption rules — see below)
└── controller/
    ├── CouponController.java            (/api/coupons/validate)
    └── AdminCouponController.java       (/api/admin/coupons/**)
```

`CouponRepository` gained `existsByCode` and `JpaSpecificationExecutor`.
`CouponUsageRepository` gained `findByCouponIdOrderByUsedAtDesc` (admin usage report).

## Endpoints

### Self-service (`/api/coupons`) — any authenticated user

| Method | Path | Description |
|---|---|---|
| POST | `/api/coupons/validate` | Check if a code would be accepted for my current cart, without redeeming it |

### Admin (`/api/admin/coupons/**`) — `ROLE_ADMIN`

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/coupons` | Create a single coupon with an explicit code |
| POST | `/api/admin/coupons/bulk-generate` | Generate up to 500 unique codes sharing the same discount rules |
| GET | `/api/admin/coupons` | Paginated, filterable (`status`, `code`) |
| GET | `/api/admin/coupons/{id}` | One coupon's detail |
| PUT | `/api/admin/coupons/{id}` | Update rules (code, discount, dates, limits) |
| PATCH | `/api/admin/coupons/{id}/deactivate` | Soft-disable (status → `INACTIVE`) |
| DELETE | `/api/admin/coupons/{id}` | Hard delete — blocked with 409 if ever redeemed |
| GET | `/api/admin/coupons/{id}/usages` | Who has redeemed this code, and on which orders |

## Design decisions worth flagging

- **Redemption logic was already built in Phase 10** (before coupon *management*
  existed, since the schema was already there) — this phase extracts that inline
  logic into `CouponValidator`, a shared, package-private class both
  `OrderServiceImpl.placeOrder` (actual redemption) and `CouponServiceImpl.validateCoupon`
  (preview) now call, instead of maintaining two copies of the same rules that
  could silently drift apart. This is the same "shared implementation detail,
  not a public service" pattern as `OrderStockCoordinator` and `PaymentRefundCoordinator`.
- **`POST /api/coupons/validate` is a preview, not a redemption** — it does not
  create a `CouponUsage` row or increment `usedCount`. It exists purely so a
  cart page can show "✓ Coupon applied, -$12.00" before the customer actually
  checks out. Only `OrderService.placeOrder` (Phase 10) redeems a code for real.
- **Coupon deletion pre-checks usage and returns a clean 409** instead of
  hitting the `coupon_usages` FK raw — the first place in the project where a
  "deletion gap" flagged in an earlier phase (Category/Brand/Product/User all
  had this same gap at the time) was actually fixed rather than repeated,
  since this module was new in Phase 12. ~~The same treatment for
  Category/Brand/Product/User is still owed.~~ **Resolved in Phase 15**: all
  four now have the identical pre-check pattern.
- **Bulk-generated codes get a per-batch collision guard** in addition to the
  DB's `UNIQUE(code)` constraint — astronomically unlikely to matter at 8
  random chars from a 33-character alphabet (ambiguous characters like `0`/`O`,
  `1`/`I` excluded), but cheap insurance against a large batch occasionally
  failing outright on a mid-batch unique-constraint violation.

---

# Phase 13 — Review

## What's new

```
src/main/java/com/ecommerce/
├── dto/review/
│   ├── CreateReviewRequest.java / UpdateReviewRequest.java
│   ├── ReviewResponse.java                    (includes verifiedPurchase — see below)
│   ├── ProductRatingSummaryResponse.java       (average, count, 1-5 star breakdown)
│   └── AdminReviewFilterRequest.java
├── mapper/
│   └── ReviewMapper.java
├── repository/specification/
│   └── ReviewSpecification.java
├── service/
│   ├── ReviewService.java + impl        (self-service authoring + public reads)
│   └── AdminReviewService.java + impl   (moderation: list any, delete any)
└── controller/
    ├── ReviewController.java            (/api/reviews/** — authoring, authenticated)
    ├── ProductReviewController.java     (/api/products/{id}/reviews/** — public reads)
    └── AdminReviewController.java       (/api/admin/reviews/** — moderation)
```

`ReviewRepository` gained pagination (`findByProductId(id, Pageable)`), an ownership
check (`findByIdAndUserId`), and rating-aggregation queries. `OrderItemRepository`
gained `existsDeliveredPurchase` — the verified-purchase gate this phase relies on.

## Endpoints

### Authoring (`/api/reviews/**`) — any authenticated user, own reviews only

| Method | Path | Description |
|---|---|---|
| POST | `/api/reviews` | Create a review. Body: `{productId, rating, comment}` |
| GET | `/api/reviews/me` | My reviews across all products |
| PUT | `/api/reviews/{id}` | Edit my own review |
| DELETE | `/api/reviews/{id}` | Delete my own review |

### Public browsing (`/api/products/{productId}/reviews/**`) — no auth required

| Method | Path | Description |
|---|---|---|
| GET | `/api/products/{productId}/reviews` | Paginated reviews for a product |
| GET | `/api/products/{productId}/reviews/summary` | Average rating, count, 1-5 star breakdown |

### Admin moderation (`/api/admin/reviews/**`) — `ROLE_ADMIN`

| Method | Path | Description |
|---|---|---|
| GET | `/api/admin/reviews` | Paginated, filterable (`productId`, `userId`, `minRating`, `maxRating`) |
| DELETE | `/api/admin/reviews/{id}` | Remove any review (moderation — no ownership check) |

## Design decisions worth flagging

- **Reviews are gated behind a verified purchase, specifically a `DELIVERED`
  order.** `OrderItemRepository.existsDeliveredPurchase` (a small dedicated
  query, not a full order fetch) checks the caller has at least one `DELIVERED`
  order containing the product before `createReview` will accept anything.
  Deliberately stricter than "ever ordered it" — a cancelled or still-in-transit
  order doesn't qualify. Given this gate, `ReviewResponse.verifiedPurchase` is
  currently *always* `true`; it's still modeled as an explicit field (rather
  than left implicit) so the frontend has a stable field to badge against if
  this gate is ever loosened later (e.g. allowing unverified reviews with a
  visible "unverified" label instead of blocking them outright).
- **Public browsing is a separate controller from authoring**
  (`ProductReviewController` vs `ReviewController`), nested under
  `/api/products/{id}/reviews` specifically so it falls under the *existing*
  `permitAll` GET rule for the product catalog in `SecurityConfig` — no
  security config changes were needed for this phase. Authoring lives at the
  flatter `/api/reviews` (not nested under `/api/products`) deliberately: a
  POST at `/api/products/**` would otherwise collide with the admin-only
  product-write rules already in `SecurityConfig`.
- **Rating summary always returns all 5 star buckets**, even ones with zero
  reviews (`{1: 0, 2: 0, 3: 4, 4: 12, 5: 30}`), so the frontend can render a
  histogram without special-casing missing keys.
- **Admin moderation is delete-only, not edit** — an admin can remove a review
  outright (e.g. abusive/spam content) but can't silently rewrite someone's
  rating or comment. If review *editing* by admins is ever needed, that's a
  deliberate omission to revisit, not an oversight.
- **`ProductResponse` (Phase 5) does not carry rating info.** Average rating
  and review count are only available via the dedicated summary endpoint, not
  embedded in every product list item — computing an aggregate per row would
  turn `GET /api/products` into an N+1 query without a schema change (e.g. a
  cached `products.average_rating` column, updated on each review write).
  Flagging this as the natural next step if the frontend needs star ratings
  visible on a product grid rather than only on the product detail page.

## Notes / things to decide in later phases

- **Denormalizing rating onto `Product`** (a cached `average_rating` +
  `review_count` pair, updated whenever a review is created/updated/deleted) is
  the obvious follow-up if Phase 19's frontend wants ratings on listing pages
  without a summary call per product. Worth doing as its own small migration +
  a write-side update in `ReviewServiceImpl` rather than bolting it on later
  under time pressure.
- **No review "helpful" voting or photo attachments** — kept deliberately
  minimal (rating + text) to match the schema from Phase 2; flag if either is
  actually wanted, since both need new tables/columns.

---

# Phase 14 — Notification

## What's new

```
src/main/java/com/ecommerce/
├── dto/notification/
│   ├── NotificationResponse.java
│   ├── SendNotificationRequest.java        (admin: one user)
│   ├── BroadcastNotificationRequest.java    (admin: every ACTIVE user)
│   ├── UnreadCountResponse.java
│   └── AdminNotificationFilterRequest.java
├── mapper/
│   └── NotificationMapper.java
├── repository/specification/
│   └── NotificationSpecification.java
├── service/
│   ├── NotificationService.java + impl        (self-service reads + the internal notify() hook)
│   └── AdminNotificationService.java + impl    (admin: send/broadcast/list)
├── controller/
│   ├── NotificationController.java             (/api/notifications/**)
│   └── AdminNotificationController.java        (/api/admin/notifications/**)
└── db/migration/
    └── V4__add_notification_type.sql
```

`Notification` gained a `type` column (`ORDER`/`PAYMENT`/`PROMOTION`/`ACCOUNT`/`GENERAL`)
via V4. `NotificationRepository` gained pagination, an ownership check
(`findByIdAndUserId`), `countByUserIdAndIsReadFalse`, and now extends
`JpaSpecificationExecutor`.

## Endpoints

### Self-service (`/api/notifications/**`) — any authenticated user, own notifications only

| Method | Path | Description |
|---|---|---|
| GET | `/api/notifications` | Paginated, newest first |
| GET | `/api/notifications/unread-count` | `{"count": N}` — for a bell-icon badge |
| PATCH | `/api/notifications/{id}/read` | Mark one as read |
| PATCH | `/api/notifications/read-all` | Mark all of mine as read |
| DELETE | `/api/notifications/{id}` | Delete one of mine |

There is deliberately no "create" endpoint here — a notification is only ever
created as a side effect of a business event or by an admin (below).

### Admin (`/api/admin/notifications/**`) — `ROLE_ADMIN`

| Method | Path | Description |
|---|---|---|
| POST | `/api/admin/notifications/send` | Send one notification to a specific user |
| POST | `/api/admin/notifications/broadcast` | Send the same notification to every `ACTIVE` user |
| GET | `/api/admin/notifications` | Paginated, filterable (`userId`, `type`, `isRead`) |

## Design decisions worth flagging

- **`NotificationService.notify(...)` is the real product of this phase** — an
  internal method, never exposed via any controller, called by other services
  as a side effect of business events. It's already wired into:
  - `OrderServiceImpl.placeOrder` → "Order placed"
  - `OrderServiceImpl.cancelMyOrder` → "Order cancelled"
  - `AdminOrderServiceImpl.updateOrderStatus` → "Order {new status}" on *any*
    transition (confirmed, shipped, delivered, cancelled, refunded)
  - `PaymentServiceImpl.processPayment` → "Payment successful" / "Payment failed"
    (a deliberately separate, `PAYMENT`-type notification from the order-status
    one — placing a successful payment on a `PENDING` order therefore produces
    *two* notifications: "Order confirmed" and "Payment successful". Considered
    and kept as-is, not a duplicate bug — they're two genuinely distinct facts
    a customer would want to see.)
- **A failed notification can never roll back the transaction that triggered
  it — and this is actually guaranteed, not just usually true.** `notify()`
  runs in `Propagation.REQUIRES_NEW` (its own transaction, independent of the
  caller's) wrapped in a try/catch that swallows and logs at `WARN`. The
  subtlety: this only *actually* prevents an order/payment failure because
  every entity in this project uses `GenerationType.IDENTITY`, so
  `notificationRepository.save(...)` issues its `INSERT` synchronously, inside
  the try block — any DB failure surfaces there, not silently deferred to a
  later flush the caller's code has no chance to catch.
- **No registration "welcome" notification is wired yet.** `AuthServiceImpl.register`
  doesn't call `notify()` — the hook is easy to add (one line, `ACCOUNT` type)
  but deliberately left out here since it wasn't clear it was wanted; flag if
  you'd like it added.
- **Broadcast is synchronous and unbatched, on purpose, for this phase's
  scale.** `broadcastToAll` fetches every `ACTIVE` user and inserts one row per
  user inside the same request/transaction — fine for a small-to-medium user
  base, but this becomes the wrong approach once the user base is large enough
  that the request would time out or the transaction would hold a lock for
  too long. Solving that (async queue, batching, retry policy) is an
  infrastructure decision explicitly deferred, not solved here.
- **Admins can send/broadcast, but never read or manage other users' notifications
  as if they were the recipient** — `AdminNotificationController` has no
  mark-as-read or delete-any endpoint; `GET /api/admin/notifications` is
  reporting/visibility only (e.g. "did user 42 get notified about their
  order?"), not an inbox an admin acts on someone else's behalf through.

## Notes / things to decide in later phases

- **This phase is in-app only — no email/SMS/push delivery.** `Notification`
  rows are purely a database-backed inbox; nothing here integrates
  `JavaMailSender`, an SMS gateway, or push notifications. If real out-of-band
  delivery is wanted, that's most naturally a second implementation behind the
  same `notify()` call site (e.g. `notify()` both saves a row *and* enqueues an
  email), not a rewrite of what's here.
- **Broadcast's synchronous approach should be revisited before Phase 18/19**
  if the user base is expected to be non-trivial by launch — repeating the
  flag above since it's the one thing in this phase most likely to need
  revisiting under real load.

---

# Phase 15 — Exception + Validation

## What's new

Most of this phase's substance was actually built incrementally, one exception
type at a time, starting back in Phase 3 — every phase since then added its own
`@ExceptionHandler` as new failure modes came up. What Phase 15 does is
consolidate all of that onto one consistent shape and close every remaining
gap that had been explicitly deferred:

```
src/main/java/com/ecommerce/
├── dto/common/
│   └── ErrorResponse.java           (the one error shape the whole API now returns)
├── exception/
│   ├── GlobalExceptionHandler.java  (consolidated — see full list below)
│   └── ResourceInUseException.java  (new — 409, for "still referenced" delete guards)
└── security/
    └── SecurityResponseHandlers.java (401/403 from the JWT filter chain, same ErrorResponse shape)
```

## `ErrorResponse` — the shape every error takes

```json
{
  "timestamp": "2026-08-22T14:03:11",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "fieldErrors": { "name": "Product name is required", "price": "Price must be greater than 0" }
}
```

`fieldErrors` is omitted entirely (not `null`, not `{}`) unless the failure was
a field-level validation error — `@JsonInclude(NON_NULL)` keeps every other
error response clean.

## Every exception `GlobalExceptionHandler` now covers

| Exception | Status | Notes |
|---|---|---|
| `ResourceNotFoundException` | 404 | This project's own — entity doesn't exist |
| `DuplicateResourceException` | 409 | Uniqueness constraint (name, email, code, ...) |
| `ResourceInUseException` | 409 | **New this phase** — "still referenced," see below |
| `InvalidRequestException` | 400 | Semantically-invalid input past bean validation |
| `InsufficientStockException` | 409 | Stock conflict at checkout |
| `UnauthorizedException` / `InvalidCredentialsException` | 401 | Auth-specific failures raised inside a service |
| `BadCredentialsException` | 401 | Spring Security, from `AuthenticationManager.authenticate()` |
| `DisabledException` / `LockedException` | 403 | Account not `ACTIVE` |
| `AccessDeniedException` | 403 | Denial raised *inside* a controller/service |
| `MethodArgumentNotValidException` | 400 | `@Valid` field failures → populates `fieldErrors` |
| `HttpMessageNotReadableException` | 400 | Malformed/unreadable JSON body |
| `MethodArgumentTypeMismatchException` | 400 | e.g. `/api/products/abc` where a number was expected |
| `MissingServletRequestParameterException` | 400 | Required `@RequestParam` omitted |
| `HttpRequestMethodNotSupportedException` | 405 | **New this phase** — wrong HTTP verb on a valid path |
| `NoHandlerFoundException` | 404 | **New this phase** — no route matches at all |
| `DataIntegrityViolationException` | 409 | **New this phase** — safety-net for any unhandled DB constraint |
| `Exception` (catch-all) | 500 | Logged with full stack trace server-side; client gets a generic message only |

Ordering note: Spring resolves `@ExceptionHandler` methods by most-specific
exception type, so e.g. `ResourceNotFoundException` always wins over the
catch-all regardless of declaration order in the file.

## Design decisions worth flagging

- **The two gaps this phase actually had to build, rather than just
  consolidate, were `HttpRequestMethodNotSupportedException` (405) and
  `NoHandlerFoundException` (404).** Without them, hitting a valid path with
  the wrong HTTP method, or a URL that matches no route at all, both fell
  through to the generic catch-all and misreported as a 500 — the wrong
  status code for what's actually a client error. `NoHandlerFoundException`
  additionally required two `application.yml` settings
  (`spring.mvc.throw-exception-if-no-handler-found` and
  `spring.web.resources.add-mappings: false`) — without them, Spring Boot
  serves its own whitelabel 404 for unmapped routes and never reaches this
  handler at all, regardless of what's registered here.
- **`ResourceInUseException` (409) is new, and retroactively fixes a status-code
  inconsistency, not just a missing handler.** Phase 12's coupon-deletion guard
  had originally used `InvalidRequestException` (400) for what was always
  conceptually a "the record is still referenced" case (409) — introducing
  this dedicated exception meant going back and correcting that call site, plus
  applying the identical pre-check pattern to the delete methods that had been
  explicitly left with raw DB constraint errors since their original phases:
  `CategoryServiceImpl.deleteCategory`, `BrandServiceImpl.deleteBrand`,
  `ProductServiceImpl.deleteProduct` (handled per-reference, not uniformly —
  see the updated Phase 5 notes), and `AdminUserServiceImpl.deleteUser`. Every
  one of these was an explicitly-tracked "pending Phase 15" item in an earlier
  phase's README section; all four are now resolved and those sections updated
  in place (struck through, not deleted, so the history of what was flagged
  and when stays visible).
- **`DataIntegrityViolationException` is a safety net, not a substitute for the
  pre-checks above.** It exists so that any reference this project *hasn't*
  explicitly pre-checked (e.g. the sub-category duplicate-name case flagged
  back in Phase 3, which still has no dedicated check) fails as a clean-ish 409
  instead of a raw 500 — but its message is necessarily generic ("referenced by
  other data"), not as specific as a real pre-check would give. Logged at
  `WARN` server-side specifically so an unhandled case like this is visible to
  a developer, not just silently swallowed.
- **`SecurityResponseHandlers` needed its own copy of the same `ErrorResponse`
  shape, not a shared call into `GlobalExceptionHandler`.** A 401 from a
  missing/invalid JWT is rejected inside Spring Security's filter chain,
  *before* the request ever reaches the `DispatcherServlet` — so it never
  becomes a Java exception that `@RestControllerAdvice` can intercept.
  `AuthenticationEntryPoint`/`AccessDeniedHandler` are Spring Security's own
  extension points for exactly this case, and both now build the identical
  `ErrorResponse` DTO directly (via a reused `ObjectMapper` instance, not a new
  one per request) so a 401/403 from the filter chain is indistinguishable, on
  the wire, from one raised inside a controller.
- **The catch-all `Exception` handler never leaks the original exception's
  message or stack trace to the client** — only a generic "An unexpected error
  occurred," with the real detail logged server-side at `ERROR`. This is the
  one place in the whole handler where being *less* informative to the client
  is the correct choice: a bug's internal detail (a NullPointerException's
  message, an SQL exception's driver-specific text) is exactly the kind of
  thing that shouldn't reach an API consumer.

## Notes / things to decide in later phases

- **Sub-category duplicate-name still has no dedicated pre-check** (flagged
  originally in Phase 3, only partially mitigated here by the generic
  `DataIntegrityViolationException` fallback) — worth a real fix if that
  collision is likely in practice, since the fallback's message is generic
  rather than specific.
- **A failed *refund* attempt still isn't modeled as its own status** (flagged
  in Phase 11) — this phase's `DataIntegrityViolationException` safety net
  doesn't touch that gap at all, since it's a business-logic decision, not an
  unhandled DB constraint. Still owed once a real payment gateway exists to
  observe an actual refund failure against.
- **Swagger (Phase 16) can now document one error schema** instead of guessing
  per-endpoint — `ErrorResponse` is stable as of this phase specifically so
  that documentation effort isn't wasted on a shape that's still shifting.

---

# Phase 16 — Swagger

## What's new

```
src/main/java/com/ecommerce/config/
└── OpenApiConfig.java   (global metadata + bearer-JWT security scheme)
```

No new controllers, services, or entities — this phase is annotations on
what already exists: every one of the ~20 controllers now carries a
class-level `@Tag`, public endpoints that sit inside an otherwise
authenticated controller are marked with `@SecurityRequirements` (overriding
the global bearer requirement for just that endpoint), and endpoints whose
behavior isn't obvious from the URL/method alone got an `@Operation`
description. `pom.xml` gained `springdoc-openapi-starter-webmvc-ui`;
`application.yml` gained a small `springdoc.swagger-ui` block for UI sorting
and auth persistence. `SecurityConfig` already had `/swagger-ui/**` and
`/v3/api-docs/**` permitted (added ahead of time, tagged `// Phase 16`, back
when `SecurityConfig` was first written).

## Where to look

- **Swagger UI**: `/swagger-ui/index.html` (or `/swagger-ui.html`, which redirects)
- **Raw OpenAPI JSON**: `/v3/api-docs`

To call an authenticated endpoint from the UI: `POST /api/auth/login`, copy
`accessToken` from the response, click **Authorize**, paste it in (no need to
type `Bearer ` — that's added automatically since the scheme is configured as
HTTP bearer).

## Design decisions worth flagging

- **`@Operation` was deliberately used sparingly, not on every endpoint.** A
  plain `GET /api/brands/{id}` needs no explanation beyond its tag — adding a
  description would just restate the URL. `@Operation` was reserved for
  endpoints where the *behavior* isn't obvious from the signature: state
  machines (`AdminOrderController.updateOrderStatus`), side effects that
  aren't in the URL (`OrderController.placeOrder` decrementing stock and
  clearing the cart, `PaymentController.processPayment` driving an order-status
  transition), preview-vs-real-effect distinctions
  (`CouponController.validateCoupon` not redeeming), and delete guards that
  return 409/400 under specific conditions. Roughly a third of endpoints got
  one; the rest rely on the class-level `@Tag` description plus a
  self-explanatory method signature.
- **`@SecurityRequirements` (empty) is the override for "this specific
  endpoint is public despite living in an otherwise-authenticated
  controller."** `OpenApiConfig` adds a *global* bearer requirement to every
  operation by default; the empty annotation clears it for just that method
  (or, on `AuthController`/`ProductReviewController`, the whole class, since
  every endpoint in each is public). This mirrors `SecurityConfig`'s actual
  authorization rules exactly — the Swagger UI's padlock icons are accurate,
  not just decorative, because both were built from the same set of "which
  endpoints are actually public" decisions made back in Phases 3–13.
- **The `Review` tag is deliberately shared by two different controller
  classes** (`ReviewController` for authoring, `ProductReviewController` for
  public browsing) rather than split into two tags. Swagger UI merges
  same-named tags from different classes into one section — chosen because a
  reader thinking "I want to know everything about Reviews" shouldn't have to
  realize that's implemented as two Java classes to find it all.
- **This phase couldn't be verified by actually running the app.** This
  sandbox's network allowlist doesn't include Maven Central, so `mvn compile`
  was never runnable at any point in this project — every phase so far has
  been written and reasoned about for correctness, not build-verified. This
  phase in particular is worth a real `mvn spring-boot:run` + a look at
  `/swagger-ui/index.html` before trusting it blindly: springdoc's own
  resource handler for the Swagger UI's static assets is expected to work
  independently of the `spring.web.resources.add-mappings: false` setting
  added in Phase 15 (they're registered through different mechanisms), but
  that's reasoned from how springdoc's autoconfiguration works, not confirmed
  by actually loading the page.

## Notes / things to decide in later phases

- **No request/response examples (`@ExampleObject`) were added anywhere.**
  Field-level descriptions come from each DTO's existing Javadoc/validation
  messages (springdoc picks those up automatically); nothing here adds
  worked examples beyond what a DTO's own field names and validation
  constraints already convey. Worth adding for the trickier request bodies
  (`PlaceOrderRequest`, `BulkGenerateCouponRequest`) if Phase 19's frontend
  team finds the schema alone insufficient.
- **No API versioning strategy exists yet** (`/api/v1/...` vs `/api/...`) —
  everything is unversioned `/api/**`. Not a Swagger-phase concern by itself,
  but worth deciding before Phase 19 locks in a frontend contract against
  URLs that might need to change shape later.
