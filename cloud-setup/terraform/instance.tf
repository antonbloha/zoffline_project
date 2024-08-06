resource "aws_instance" "zwift" {
  ami                    = "ami-053b0d53c279acc90" # Ubuntu server 22.04
  instance_type          = "c6a.large"             # "c6a.large" # Do not forget to turn off the instance after the test (56$ per month)
  vpc_security_group_ids = [aws_security_group.web-sg.id]
  key_name               = "ssh_key_devops"
  iam_instance_profile   = aws_iam_instance_profile.zwift_role.name

  tags = {
    Name = "Zwift_Project"
  }
}

