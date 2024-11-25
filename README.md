# Job Portal App Backend

## API Endpoints

### Get All Job Posts
- **URL:** `/jobPosts`
- **Method:** `GET`
- **Description:** Retrieves a list of all job posts.
- **Response:**
    - `200 OK` with a list of job posts.

### Get Job Post by ID
- **URL:** `/jobPost/{postId}`
- **Method:** `GET`
- **Description:** Retrieves a job post by its ID.
- **Path Variables:**
    - `postId` (int): The ID of the job post.
- **Response:**
    - `200 OK` with the job post details.
    - `404 Not Found` if the job post does not exist.

### Add a New Job Post
- **URL:** `/jobPost`
- **Method:** `POST`
- **Description:** Adds a new job post.
- **Request Body:**
    - JSON object representing the job post.
- **Response:**
    - `201 Created` with the created job post details.

### Update an Existing Job Post
- **URL:** `/jobPost`
- **Method:** `PUT`
- **Description:** Updates an existing job post.
- **Request Body:**
    - JSON object representing the updated job post.
- **Response:**
    - `200 OK` with the updated job post details.
    - `404 Not Found` if the job post does not exist.

### Delete a Job Post
- **URL:** `/jobPost/{postId}`
- **Method:** `DELETE`
- **Description:** Deletes a job post by its ID.
- **Path Variables:**
    - `postId` (int): The ID of the job post.
- **Response:**
    - `200 OK` with a success message.
    - `404 Not Found` if the job post does not exist.

### Load Sample Data
- **URL:** `/loadData`
- **Method:** `GET`
- **Description:** Loads sample job posts data into the database.
- **Response:**
    - `200 OK` with a success message.

### Search Job Posts
- **URL:** `/jobPosts/search/{keyword}`
- **Method:** `GET`
- **Description:** Searches job posts by a keyword in the profile or description.
- **Path Variables:**
    - `keyword` (String): The keyword to search for.
- **Response:**
    - `200 OK` with a list of matching job posts.