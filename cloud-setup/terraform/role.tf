
# Create IAM role for Jenkins
resource "aws_iam_instance_profile" "zwift_role" {
  name = "zwift_offline_project"
  role = aws_iam_role.zwift_profile.name
}

resource "aws_iam_role" "zwift_role" {
  name = "zwift_offline_project"

  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = "sts:AssumeRole",
        Effect = "Allow",
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "zwift_role" {
  name = "zwift_offline_project"
  role = aws_iam_role.zwift_role.id

  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Action = [
          "ec2:*"
        ],
        Effect   = "Allow",
        Resource = "*"
      },
      {
        Action = [
          "s3:*"
        ],
        Effect   = "Allow",
        Resource = "*"
      },
      {
        Action = [
          "iam:PassRole",
          "iam:ListRoles"
        ],
        Effect   = "Allow",
        Resource = "*"
      }
    ]
  })
}
